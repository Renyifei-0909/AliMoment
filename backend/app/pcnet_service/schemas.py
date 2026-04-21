from __future__ import annotations

from typing import Optional

from pydantic import BaseModel, Field


class PCNetInferRequest(BaseModel):
    video_id: str = Field(..., description="Video id stored in the PC-Net feature HDF5 file")
    duration: float = Field(..., gt=0, description="Video duration in seconds")
    query: str = Field(..., min_length=1, description="Natural-language query")
    top_k: Optional[int] = Field(None, ge=1, le=8, description="Number of proposals to return")
    use_vote: Optional[bool] = Field(None, description="Whether to enable vote-based proposal selection")


class PCNetProposal(BaseModel):
    rank: int
    start: float
    end: float
    normalized_start: float
    normalized_end: float
    reconstruction_loss: float


class PCNetInferResponse(BaseModel):
    video_id: str
    duration: float
    query: str
    dataset: str
    top_prediction: PCNetProposal
    proposals: list[PCNetProposal]


class PCNetStatusResponse(BaseModel):
    ready: bool
    dataset: Optional[str] = None
    device: Optional[str] = None
    config_path: Optional[str] = None
    checkpoint_path: Optional[str] = None
    feature_path: Optional[str] = None
    message: Optional[str] = None
