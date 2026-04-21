from __future__ import annotations

from fastapi import APIRouter, HTTPException

from app.api.schemas import SearchRequest, SearchResponse
from app.pcnet_service.service import PCNetServiceError
from app.services.asset_service import AssetServiceError
from app.services.search_service import get_search_service

router = APIRouter(prefix="/api", tags=["search"])


@router.post("/search", response_model=SearchResponse)
def search(payload: SearchRequest) -> dict:
    try:
        result = get_search_service().search(
            asset_id=payload.asset_id,
            query=payload.query,
            top_k=payload.top_k,
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
