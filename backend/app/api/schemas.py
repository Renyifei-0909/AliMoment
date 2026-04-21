from __future__ import annotations

from typing import Dict, List, Optional

from pydantic import BaseModel, Field


class ApiResponse(BaseModel):
    status: str = "success"


class AssetSummary(BaseModel):
    asset_id: str
    title: str
    duration: float
    status: str
    dataset: str
    thumbnail_url: str = ""
    suggested_queries: List[str] = Field(default_factory=list)


class AssetsResponse(ApiResponse):
    data: List[AssetSummary]


class SearchRequest(BaseModel):
    asset_id: str = Field(..., min_length=1)
    query: str = Field(..., min_length=1)
    top_k: Optional[int] = Field(None, ge=1, le=8)


class SearchHit(BaseModel):
    start_time: float
    end_time: float
    score: float
    rank: int


class SearchResponseData(BaseModel):
    asset_id: str
    source_video_id: str
    original_query: str
    translated_query: str
    normalization_mode: str
    results: List[SearchHit]
    debug: Dict[str, str] = Field(default_factory=dict)


class SearchResponse(ApiResponse):
    data: SearchResponseData
