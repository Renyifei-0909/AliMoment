from __future__ import annotations

import os
from pathlib import Path


def resolve_repo_root() -> Path:
    # backend/app/integrations/narrato/runtime_bootstrap.py -> repo root
    return Path(__file__).resolve().parents[4]


def default_narrato_root() -> Path:
    return resolve_repo_root() / "third_party" / "narratoai"


def resolve_narrato_root(configured_path: str | None = None) -> Path:
    raw = (configured_path or os.getenv("NARRATO_SUBMODULE_PATH", "")).strip()
    root = Path(raw) if raw else default_narrato_root()
    return root.resolve()


def validate_narrato_root(narrato_root: Path) -> None:
    required = [
        narrato_root / "app" / "services" / "task.py",
        narrato_root / "app" / "services" / "documentary" / "frame_analysis_service.py",
        narrato_root / "config.example.toml",
    ]
    missing = [str(path) for path in required if not path.exists()]
    if missing:
        raise RuntimeError(f"NarratoAI 子模块目录无效，缺少文件: {missing}")


def ensure_narrato_import_path(narrato_root: Path, *, remove_alimoment_backend: bool = False) -> None:
    narrato_root_str = str(narrato_root)
    if narrato_root_str not in os.sys.path:
        os.sys.path.insert(0, narrato_root_str)

    if not remove_alimoment_backend:
        return

    repo_root = resolve_repo_root()
    backend_root = repo_root / "backend"
    removable = {"", str(repo_root), str(backend_root)}
    os.sys.path[:] = [entry for entry in os.sys.path if entry not in removable]
