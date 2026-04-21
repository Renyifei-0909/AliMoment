from __future__ import annotations

from typing import List

from fastapi import APIRouter, Body, HTTPException, Query

from app.api.schemas import EditRequest, EditResponse, MediaUploadResponse
from app.services.media_service import MediaServiceError, get_media_service

router = APIRouter(prefix="/api", tags=["media"])


@router.post("/media/upload", response_model=MediaUploadResponse)
def upload_media(
    raw_body: bytes = Body(...),
    filename: str = Query(..., min_length=1),
) -> dict:
    try:
        uploaded = get_media_service().save_upload(original_filename=filename, content=raw_body)
    except MediaServiceError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc

    return {
        "status": "success",
        "data": {
            "media_id": uploaded.media_id,
            "filename": uploaded.filename,
            "original_filename": uploaded.original_filename,
            "file_size": uploaded.file_size,
        },
    }


@router.post("/edit", response_model=EditResponse)
def edit_media(payload: EditRequest) -> dict:
    try:
        result = get_media_service().edit(
            media_id=payload.media_id,
            segments=[segment.model_dump() for segment in payload.segments],
            speed=payload.options.speed,
            effect_intensity=payload.options.effect_intensity,
            add_voiceover=payload.options.add_voiceover,
        )
    except MediaServiceError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc

    return {
        "status": "success",
        "data": {
            "media_id": result.media_id,
            "output_filename": result.output_filename,
            "output_relative_url": result.output_relative_url,
            "output_url": result.output_url,
            "segments": payload.segments,
            "speed": result.speed,
            "effect_intensity": result.effect_intensity,
            "voiceover_requested": result.voiceover_requested,
            "voiceover_applied": result.voiceover_applied,
            "note": result.note,
        },
    }
