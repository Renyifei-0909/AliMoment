from __future__ import annotations

from fastapi import APIRouter, HTTPException

from app.pcnet_service.schemas import (
    PCNetInferRequest,
    PCNetInferResponse,
    PCNetStatusResponse,
)
from app.pcnet_service.service import PCNetServiceError, get_pcnet_service

router = APIRouter(prefix="/pcnet", tags=["pcnet"])


@router.get("/health", response_model=PCNetStatusResponse)
def pcnet_health() -> dict:
    service = get_pcnet_service()
    try:
        service.load()
    except Exception:
        return service.describe()
    return service.describe()


@router.post("/reload", response_model=PCNetStatusResponse)
def pcnet_reload() -> dict:
    service = get_pcnet_service()
    try:
        return service.reload()
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc)) from exc


@router.post("/infer", response_model=PCNetInferResponse)
def pcnet_infer(payload: PCNetInferRequest) -> dict:
    service = get_pcnet_service()
    try:
        result = service.infer(
            video_id=payload.video_id,
            duration=payload.duration,
            query=payload.query,
            top_k=payload.top_k,
            use_vote=payload.use_vote,
        )
    except PCNetServiceError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc)) from exc

    return {
        "video_id": result.video_id,
        "duration": result.duration,
        "query": result.query,
        "dataset": result.dataset,
        "top_prediction": result.top_prediction,
        "proposals": result.proposals,
    }
