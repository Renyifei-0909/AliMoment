from __future__ import annotations

import asyncio
import json
import sys
import traceback
from pathlib import Path
from typing import Any, Dict

from config_mapper import ensure_runtime_config
from runtime_bootstrap import ensure_narrato_import_path, resolve_narrato_root, validate_narrato_root


def _read_payload() -> Dict[str, Any]:
    raw = sys.stdin.read().strip()
    if not raw:
        return {}
    return json.loads(raw)


def _bootstrap_narrato() -> Path:
    narrato_root = resolve_narrato_root()
    validate_narrato_root(narrato_root)
    ensure_runtime_config(narrato_root)
    ensure_narrato_import_path(narrato_root, remove_alimoment_backend=True)
    return narrato_root


def _run_documentary(payload: Dict[str, Any]) -> Dict[str, Any]:
    _bootstrap_narrato()

    from app.services.documentary.frame_analysis_service import DocumentaryFrameAnalysisService

    service = DocumentaryFrameAnalysisService()
    result = asyncio.run(
        service.generate_documentary_script(
            video_path=payload["video_path"],
            video_theme=payload.get("video_theme", ""),
            custom_prompt=payload.get("custom_prompt", ""),
            frame_interval_input=payload.get("frame_interval_input"),
            vision_batch_size=payload.get("vision_batch_size"),
            vision_llm_provider=payload.get("vision_llm_provider"),
            max_concurrency=payload.get("vision_max_concurrency"),
        )
    )
    return {"script": result}


def _run_compose(payload: Dict[str, Any]) -> Dict[str, Any]:
    _bootstrap_narrato()

    from app.models.schema import VideoClipParams
    from app.services import task as task_service
    from app.services import state as state_service

    params = VideoClipParams(**payload["params"])
    task_id = payload["task_id"]
    task_service.start_subclip_unified(task_id=task_id, params=params)
    state = state_service.state.get_task(task_id) or {}
    return {"task": state}


def main() -> int:
    action = sys.argv[1] if len(sys.argv) > 1 else ""
    payload = _read_payload()
    try:
        if action == "documentary":
            data = _run_documentary(payload)
        elif action == "compose":
            data = _run_compose(payload)
        else:
            raise ValueError(f"Unsupported action: {action}")
        sys.stdout.write(json.dumps({"ok": True, "data": data}, ensure_ascii=False))
        return 0
    except Exception as exc:
        sys.stdout.write(
            json.dumps(
                {
                    "ok": False,
                    "error": str(exc),
                    "traceback": traceback.format_exc(),
                },
                ensure_ascii=False,
            )
        )
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
