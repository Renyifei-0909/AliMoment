from __future__ import annotations

import json
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Dict, List, Optional

from app.config import settings


class AssetServiceError(RuntimeError):
    pass


@dataclass
class DemoAsset:
    asset_id: str
    source_video_id: str
    dataset: str
    title: str
    duration: float
    video_path: str
    status: str
    enabled: bool
    suggested_queries: List[str]
    query_aliases: Dict[str, str]
    thumbnail_url: str = ""

    @classmethod
    def from_dict(cls, payload: Dict[str, Any]) -> "DemoAsset":
        return cls(
            asset_id=str(payload["asset_id"]),
            source_video_id=str(payload["source_video_id"]),
            dataset=str(payload.get("dataset", "ActivityNet")),
            title=str(payload["title"]),
            duration=float(payload["duration"]),
            video_path=str(payload.get("video_path", "")),
            status=str(payload.get("status", "metadata_ready")),
            enabled=bool(payload.get("enabled", True)),
            suggested_queries=list(payload.get("suggested_queries", [])),
            query_aliases=dict(payload.get("query_aliases", {})),
            thumbnail_url=str(payload.get("thumbnail_url", "")),
        )


class AssetService:
    def __init__(self) -> None:
        self._manifest_path = Path(settings.demo_assets_manifest_path).expanduser().resolve()

    def list_enabled_assets(self) -> List[DemoAsset]:
        assets = self._load_manifest()
        return [asset for asset in assets.values() if asset.enabled]

    def get_asset(self, asset_id: str) -> DemoAsset:
        assets = self._load_manifest()
        asset = assets.get(asset_id)
        if asset is None:
            raise AssetServiceError(f"Unknown asset_id: {asset_id}")
        if not asset.enabled:
            raise AssetServiceError(f"Asset is disabled: {asset_id}")
        return asset

    def _load_manifest(self) -> Dict[str, DemoAsset]:
        if not self._manifest_path.exists():
            raise AssetServiceError(f"Assets manifest not found: {self._manifest_path}")

        with open(self._manifest_path, "r", encoding="utf-8") as manifest_file:
            raw_assets = json.load(manifest_file)

        assets: Dict[str, DemoAsset] = {}
        for asset_id, payload in raw_assets.items():
            payload = dict(payload)
            payload.setdefault("asset_id", asset_id)
            assets[asset_id] = DemoAsset.from_dict(payload)
        return assets


_asset_service: Optional[AssetService] = None


def get_asset_service() -> AssetService:
    global _asset_service
    if _asset_service is None:
        _asset_service = AssetService()
    return _asset_service
