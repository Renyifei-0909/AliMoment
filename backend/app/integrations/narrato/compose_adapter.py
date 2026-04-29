from __future__ import annotations

import json
import os
import shutil
import subprocess
import sys
import threading
import uuid
from datetime import datetime
from pathlib import Path
from typing import Any, Dict, List

from app.config import settings
from app.integrations.narrato.models import NarratoTaskStatus
from app.integrations.narrato.runtime_bootstrap import resolve_narrato_root, validate_narrato_root


class NarratoComposeError(RuntimeError):
    pass


class NarratoComposeAdapter:
    def __init__(self) -> None:
        self._runner_path = Path(__file__).resolve().parent / "runner.py"
        self._tasks: Dict[str, NarratoTaskStatus] = {}
        self._lock = threading.Lock()

    def start_compose(self, *, params: Dict[str, Any]) -> str:
        task_id = str(uuid.uuid4())
        with self._lock:
            self._tasks[task_id] = NarratoTaskStatus(
                task_id=task_id,
                state="queued",
                progress=0,
                message="任务已创建，等待执行",
            )

        thread = threading.Thread(
            target=self._run_compose_in_background,
            kwargs={"task_id": task_id, "params": params},
            daemon=True,
        )
        thread.start()
        return task_id

    def run_compose_blocking(self, *, task_id: str, params: Dict[str, Any]) -> Dict[str, Any]:
        payload = {"task_id": task_id, "params": params}
        result = self._run_runner("compose", payload)
        task_data = result.get("task") or {}
        videos = task_data.get("videos") or []
        copied_paths, output_urls = self._copy_outputs(task_id=task_id, source_paths=videos)
        return {
            "state": "complete",
            "videos": copied_paths,
            "output_urls": output_urls,
            "raw_task": task_data,
        }

    def get_task(self, task_id: str) -> NarratoTaskStatus | None:
        with self._lock:
            return self._tasks.get(task_id)

    def _run_compose_in_background(self, *, task_id: str, params: Dict[str, Any]) -> None:
        self._set_task_state(task_id, state="running", progress=15, message="Narrato 合成任务运行中")
        try:
            result = self.run_compose_blocking(task_id=task_id, params=params)
            self._set_task_state(
                task_id,
                state="complete",
                progress=100,
                message="Narrato 合成完成",
                videos=result["videos"],
                output_urls=result["output_urls"],
                debug={"raw_task": result.get("raw_task", {})},
            )
        except Exception as exc:
            self._set_task_state(
                task_id,
                state="failed",
                progress=100,
                message=str(exc),
            )

    def _set_task_state(
        self,
        task_id: str,
        *,
        state: str,
        progress: int,
        message: str,
        videos: List[str] | None = None,
        output_urls: List[str] | None = None,
        debug: Dict[str, Any] | None = None,
    ) -> None:
        with self._lock:
            task = self._tasks.get(task_id)
            if task is None:
                task = NarratoTaskStatus(task_id=task_id, state=state, progress=progress, message=message)
                self._tasks[task_id] = task
            task.state = state
            task.progress = progress
            task.message = message
            task.updated_at = datetime.utcnow().isoformat()
            if videos is not None:
                task.videos = videos
            if output_urls is not None:
                task.output_urls = output_urls
            if debug is not None:
                task.debug = debug

    def _copy_outputs(self, *, task_id: str, source_paths: List[str]) -> tuple[List[str], List[str]]:
        copied_paths: List[str] = []
        output_urls: List[str] = []
        outputs_dir = Path(settings.storage_outputs_dir)
        outputs_dir.mkdir(parents=True, exist_ok=True)

        for index, source in enumerate(source_paths, start=1):
            source_path = Path(source)
            if not source_path.exists():
                continue
            target_name = f"narrato_{task_id}_{index:02d}{source_path.suffix or '.mp4'}"
            target_path = outputs_dir / target_name
            shutil.copy2(source_path, target_path)
            copied_paths.append(str(target_path))
            output_urls.append(f"{settings.backend_public_base_url.rstrip('/')}/media/outputs/{target_name}")
        return copied_paths, output_urls

    def _build_runner_env(self) -> Dict[str, str]:
        narrato_root = resolve_narrato_root(settings.narrato_submodule_path)
        validate_narrato_root(narrato_root)
        env = os.environ.copy()
        env.update(
            {
                "NARRATO_SUBMODULE_PATH": str(narrato_root),
                "NARRATO_TEXT_PROVIDER": settings.narrato_text_provider,
                "NARRATO_TEXT_API_KEY": settings.narrato_text_api_key,
                "NARRATO_TEXT_MODEL": settings.narrato_text_model,
                "NARRATO_TEXT_BASE_URL": settings.narrato_text_base_url,
                "NARRATO_VISION_PROVIDER": settings.narrato_vision_provider,
                "NARRATO_VISION_API_KEY": settings.narrato_vision_api_key,
                "NARRATO_VISION_MODEL": settings.narrato_vision_model,
                "NARRATO_VISION_BASE_URL": settings.narrato_vision_base_url,
                "NARRATO_FFMPEG_PATH": settings.narrato_ffmpeg_path,
                "NARRATO_N_THREADS": str(settings.narrato_n_threads),
                "NARRATO_FRAME_INTERVAL": str(settings.narrato_frame_interval),
                "NARRATO_VISION_BATCH_SIZE": str(settings.narrato_vision_batch_size),
                "NARRATO_VISION_MAX_CONCURRENCY": str(settings.narrato_vision_max_concurrency),
                "NARRATO_TTS_ENGINE": settings.narrato_tts_engine,
                "NARRATO_VOICE_NAME": settings.narrato_voice_name,
            }
        )
        return env

    def _run_runner(self, action: str, payload: Dict[str, Any]) -> Dict[str, Any]:
        env = self._build_runner_env()
        process = subprocess.run(
            [sys.executable, str(self._runner_path), action],
            input=json.dumps(payload, ensure_ascii=False),
            text=True,
            capture_output=True,
            env=env,
            cwd=str(settings.backend_root),
            check=False,
        )
        if process.returncode != 0:
            stderr = process.stderr.strip()
            stdout = process.stdout.strip()
            raise NarratoComposeError(stderr or stdout or "Narrato compose 子进程执行失败")
        try:
            data = json.loads(process.stdout or "{}")
        except json.JSONDecodeError as exc:
            raise NarratoComposeError(f"Narrato compose 返回非 JSON: {process.stdout}") from exc
        if not data.get("ok"):
            raise NarratoComposeError(data.get("error") or "Narrato compose 执行失败")
        return data.get("data", {})


_compose_adapter: NarratoComposeAdapter | None = None


def get_compose_adapter() -> NarratoComposeAdapter:
    global _compose_adapter
    if _compose_adapter is None:
        _compose_adapter = NarratoComposeAdapter()
    return _compose_adapter
