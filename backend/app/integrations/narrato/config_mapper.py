from __future__ import annotations

import os
from pathlib import Path
from typing import Any, Dict

import toml


def _get_env(name: str, default: str = "") -> str:
    return os.getenv(name, default).strip()


def _deep_set(target: Dict[str, Any], section: str, key: str, value: Any) -> None:
    node = target.setdefault(section, {})
    node[key] = value


def build_runtime_overrides() -> Dict[str, Any]:
    text_provider = _get_env("NARRATO_TEXT_PROVIDER", "openai")
    vision_provider = _get_env("NARRATO_VISION_PROVIDER", "openai")

    overrides: Dict[str, Any] = {
        "app": {
            "text_llm_provider": text_provider,
            "vision_llm_provider": vision_provider,
            "text_openai_api_key": _get_env("NARRATO_TEXT_API_KEY"),
            "text_openai_model_name": _get_env("NARRATO_TEXT_MODEL"),
            "text_openai_base_url": _get_env("NARRATO_TEXT_BASE_URL"),
            "vision_openai_api_key": _get_env("NARRATO_VISION_API_KEY"),
            "vision_openai_model_name": _get_env("NARRATO_VISION_MODEL"),
            "vision_openai_base_url": _get_env("NARRATO_VISION_BASE_URL"),
            "ffmpeg_path": _get_env("NARRATO_FFMPEG_PATH"),
            "n_threads": int(_get_env("NARRATO_N_THREADS", "4") or 4),
            "hide_config": True,
        },
        "proxy": {
            "enabled": (_get_env("NARRATO_PROXY_ENABLED", "false").lower() in {"1", "true", "yes", "on"}),
            "http": _get_env("NARRATO_PROXY_HTTP"),
            "https": _get_env("NARRATO_PROXY_HTTPS"),
        },
        "ui": {
            "tts_engine": _get_env("NARRATO_TTS_ENGINE", "edge_tts"),
            "edge_voice_name": _get_env("NARRATO_VOICE_NAME", "zh-CN-XiaoyiNeural-Female"),
        },
        "frames": {
            "frame_interval_input": int(_get_env("NARRATO_FRAME_INTERVAL", "3") or 3),
            "vision_batch_size": int(_get_env("NARRATO_VISION_BATCH_SIZE", "10") or 10),
            "vision_max_concurrency": int(_get_env("NARRATO_VISION_MAX_CONCURRENCY", "2") or 2),
        },
    }
    return overrides


def ensure_runtime_config(narrato_root: Path) -> Path:
    config_path = narrato_root / "config.toml"
    example_path = narrato_root / "config.example.toml"
    if not config_path.exists():
        if not example_path.exists():
            raise RuntimeError(f"NarratoAI 配置模板不存在: {example_path}")
        config_path.write_text(example_path.read_text(encoding="utf-8"), encoding="utf-8")

    config = toml.loads(config_path.read_text(encoding="utf-8"))
    overrides = build_runtime_overrides()
    for section, values in overrides.items():
        section_map = config.setdefault(section, {})
        section_map.update(values)

    config_path.write_text(toml.dumps(config), encoding="utf-8")
    return config_path
