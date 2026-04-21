from __future__ import annotations

from fastapi import APIRouter, HTTPException

from app.api.schemas import AssetsResponse
from app.services.asset_service import AssetServiceError, get_asset_service

router = APIRouter(prefix="/api", tags=["assets"])


@router.get("/assets", response_model=AssetsResponse)
def list_assets() -> dict:
    try:
        assets = get_asset_service().list_enabled_assets()
    except AssetServiceError as exc:
        raise HTTPException(status_code=500, detail=str(exc)) from exc

    return {
        "status": "success",
        "data": [
            {
                "asset_id": asset.asset_id,
                "title": asset.title,
                "duration": asset.duration,
                "status": asset.status,
                "dataset": asset.dataset,
                "thumbnail_url": asset.thumbnail_url,
                "suggested_queries": asset.suggested_queries,
            }
            for asset in assets
        ],
    }
