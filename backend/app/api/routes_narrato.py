from __future__ import annotations

from dataclasses import asdict

from fastapi import APIRouter, HTTPException

from app.api.schemas import (
    NarratoComposeRequest,
    NarratoComposeResponse,
    NarratoDocumentaryRequest,
    NarratoDocumentaryResponse,
    NarratoTaskResponse,
)
from app.integrations.narrato.compose_adapter import NarratoComposeError, get_compose_adapter
from app.integrations.narrato.documentary_adapter import NarratoDocumentaryError, get_documentary_adapter

router = APIRouter(prefix="/api/narrato", tags=["narrato"])


@router.post("/script/documentary", response_model=NarratoDocumentaryResponse)
def generate_documentary_script(payload: NarratoDocumentaryRequest) -> dict:
    try:
        script = get_documentary_adapter().generate_script(
            video_path=payload.video_path,
            video_theme=payload.video_theme,
            custom_prompt=payload.custom_prompt,
            frame_interval_input=payload.frame_interval_input,
            vision_batch_size=payload.vision_batch_size,
            vision_llm_provider=payload.vision_llm_provider,
            vision_max_concurrency=payload.vision_max_concurrency,
        )
    except NarratoDocumentaryError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc)) from exc
    return {"status": "success", "data": {"script": script}}


@router.post("/compose", response_model=NarratoComposeResponse)
def start_compose(payload: NarratoComposeRequest) -> dict:
    try:
        task_id = get_compose_adapter().start_compose(params=payload.model_dump())
    except NarratoComposeError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc)) from exc
    return {"status": "success", "data": {"task_id": task_id}}


@router.get("/tasks/{task_id}", response_model=NarratoTaskResponse)
def get_task(task_id: str) -> dict:
    task = get_compose_adapter().get_task(task_id)
    if task is None:
        raise HTTPException(status_code=404, detail="task not found")
    return {"status": "success", "data": asdict(task)}
