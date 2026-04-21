from __future__ import annotations

from pydantic import BaseModel, Field


class PCNetInferRequest(BaseModel):
    video_id: str = Field(..., description="Video id stored in the PC-Net feature HDF5 file")
    duration: float = Field(..., gt=0, description="Video duration in seconds")
    query: str = Field(..., min_length=1, description="Natural-language query")
    top_k: int | None = Field(None, ge=1, le=8, description="Number of proposals to return")
    use_vote: bool | None = Field(None, description="Whether to enable vote-based proposal selection")


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
    dataset: str | None = None
    device: str | None = None
    config_path: str | None = None
    checkpoint_path: str | None = None
    feature_path: str | None = None
    message: str | None = None
