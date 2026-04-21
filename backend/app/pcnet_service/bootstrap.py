from __future__ import annotations

import sys
from pathlib import Path


def ensure_pcnet_importable(pcnet_root: str | Path) -> Path:
    root = Path(pcnet_root).expanduser().resolve()
    if not root.exists():
        raise FileNotFoundError(f"PC-Net root does not exist: {root}")

    root_str = str(root)
    if root_str not in sys.path:
        sys.path.insert(0, root_str)
    return root
