from __future__ import annotations

from typing import Optional

from fastapi import APIRouter, Body, HTTPException, Query

from app.api.schemas import SearchRequest, SearchResponse
from app.pcnet_service.service import PCNetServiceError
from app.services.asset_service import AssetServiceError
from app.services.search_service import get_search_service

router = APIRouter(prefix="/api", tags=["search"])


@router.post("/search", response_model=SearchResponse)
def search(
    payload: Optional[SearchRequest] = Body(None),
    asset_id: Optional[str] = Query(None),
    query: Optional[str] = Query(None),
    top_k: Optional[int] = Query(None),
) -> dict:
    resolved_asset_id = payload.asset_id if payload is not None else asset_id
    resolved_query = payload.query if payload is not None else query
    resolved_top_k = payload.top_k if payload is not None else top_k

    if not resolved_asset_id or not str(resolved_asset_id).strip():
        raise HTTPException(status_code=422, detail="asset_id is required")
    if not resolved_query or not str(resolved_query).strip():
        raise HTTPException(status_code=422, detail="query is required")

    try:
        result = get_search_service().search(
            asset_id=str(resolved_asset_id).strip(),
            query=str(resolved_query).strip(),
            top_k=resolved_top_k,
        )
    except (AssetServiceError, PCNetServiceError) as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc)) from exc

    return {
        "status": "success",
        "data": {
            "asset_id": result.asset.asset_id,
            "source_video_id": result.asset.source_video_id,
            "original_query": result.normalization.original_query,
            "translated_query": result.normalization.translated_query,
            "normalization_mode": result.normalization.normalization_mode,
            "results": result.results,
            "debug": {
                "dataset": result.asset.dataset,
                "asset_status": result.asset.status,
                "query_normalizer_mode": result.normalization.normalization_mode,
            },
        },
    }
