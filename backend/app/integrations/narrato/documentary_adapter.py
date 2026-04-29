from __future__ import annotations

import json
import os
import subprocess
import sys
from pathlib import Path
from typing import Any, Dict, List

from app.config import settings
from app.integrations.narrato.runtime_bootstrap import resolve_narrato_root, validate_narrato_root


class NarratoDocumentaryError(RuntimeError):
    pass


class NarratoDocumentaryAdapter:
    def __init__(self) -> None:
        self._runner_path = Path(__file__).resolve().parent / "runner.py"

    def generate_script(
        self,
        *,
        video_path: str,
        video_theme: str = "",
        custom_prompt: str = "",
        frame_interval_input: int | None = None,
        vision_batch_size: int | None = None,
        vision_llm_provider: str | None = None,
        vision_max_concurrency: int | None = None,
    ) -> List[Dict[str, Any]]:
        payload = {
            "video_path": video_path,
            "video_theme": video_theme,
            "custom_prompt": custom_prompt,
            "frame_interval_input": frame_interval_input,
            "vision_batch_size": vision_batch_size,
            "vision_llm_provider": vision_llm_provider,
            "vision_max_concurrency": vision_max_concurrency,
        }
        result = self._run_runner("documentary", payload)
        script = result.get("script")
        if not isinstance(script, list):
            raise NarratoDocumentaryError("Narrato documentary 返回结果格式错误")
        return script

    def _run_runner(self, action: str, payload: Dict[str, Any]) -> Dict[str, Any]:
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
            raise NarratoDocumentaryError(stderr or stdout or "Narrato documentary 子进程执行失败")

        try:
            data = json.loads(process.stdout or "{}")
        except json.JSONDecodeError as exc:
            raise NarratoDocumentaryError(f"Narrato documentary 返回非 JSON: {process.stdout}") from exc

        if not data.get("ok"):
            raise NarratoDocumentaryError(data.get("error") or "Narrato documentary 执行失败")
        return data.get("data", {})


_documentary_adapter: NarratoDocumentaryAdapter | None = None


def get_documentary_adapter() -> NarratoDocumentaryAdapter:
    global _documentary_adapter
    if _documentary_adapter is None:
        _documentary_adapter = NarratoDocumentaryAdapter()
    return _documentary_adapter
