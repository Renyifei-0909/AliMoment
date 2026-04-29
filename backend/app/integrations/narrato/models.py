from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime
from typing import Any, Dict, List


@dataclass
class NarratoTaskStatus:
    task_id: str
    state: str
    progress: int
    message: str = ""
    videos: List[str] = field(default_factory=list)
    output_urls: List[str] = field(default_factory=list)
    created_at: str = field(default_factory=lambda: datetime.utcnow().isoformat())
    updated_at: str = field(default_factory=lambda: datetime.utcnow().isoformat())
    debug: Dict[str, Any] = field(default_factory=dict)
