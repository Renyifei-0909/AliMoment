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


class MediaUploadResponseData(BaseModel):
    media_id: str
    filename: str
    original_filename: str
    file_size: int
    preview_filename: str
    preview_relative_url: str
    preview_url: str
    preview_note: str = ""


class MediaUploadResponse(ApiResponse):
    data: MediaUploadResponseData


class EditSegment(BaseModel):
    start: float = Field(..., ge=0)
    end: float = Field(..., ge=0)


class EditOptions(BaseModel):
    speed: float = Field(1.0, ge=0.5, le=3.0)
    effect_intensity: float = Field(0.0, ge=0.0, le=100.0)
    demand: str = ""
    add_voiceover: bool = False


class EditRequest(BaseModel):
    media_id: str = Field(..., min_length=1)
    segments: List[EditSegment] = Field(..., min_length=1)
    options: EditOptions = Field(default_factory=EditOptions)


class EditResponseData(BaseModel):
    media_id: str
    output_filename: str
    output_relative_url: str
    output_url: str
    segments: List[EditSegment]
    speed: float
    effect_intensity: float
    voiceover_requested: bool
    voiceover_applied: bool
    note: str = ""


class EditResponse(ApiResponse):
    data: EditResponseData


class NarratoDocumentaryRequest(BaseModel):
    video_path: str = Field(..., min_length=1)
    video_theme: str = ""
    custom_prompt: str = ""
    frame_interval_input: Optional[int] = Field(None, ge=1)
    vision_batch_size: Optional[int] = Field(None, ge=1)
    vision_llm_provider: Optional[str] = None
    vision_max_concurrency: Optional[int] = Field(None, ge=1)


class NarratoDocumentaryResponseData(BaseModel):
    script: List[Dict[str, object]] = Field(default_factory=list)


class NarratoDocumentaryResponse(ApiResponse):
    data: NarratoDocumentaryResponseData


class NarratoComposeRequest(BaseModel):
    video_clip_json_path: str = Field(..., min_length=1)
    video_origin_path: str = Field(..., min_length=1)
    tts_engine: str = "edge_tts"
    voice_name: str = "zh-CN-XiaoyiNeural-Female"
    voice_rate: float = 1.0
    voice_pitch: float = 1.0
    subtitle_enabled: bool = False
    font_name: str = "Microsoft YaHei"
    font_size: int = 24
    text_fore_color: str = "#FFFFFF"
    subtitle_position: str = "bottom"
    custom_position: float = 70.0
    n_threads: int = 4
    video_aspect: str = "16:9"
    tts_volume: float = 1.0
    original_volume: float = 1.0
    bgm_volume: float = 0.3


class NarratoComposeResponseData(BaseModel):
    task_id: str


class NarratoComposeResponse(ApiResponse):
    data: NarratoComposeResponseData


class NarratoTaskResponseData(BaseModel):
    task_id: str
    state: str
    progress: int
    message: str = ""
    videos: List[str] = Field(default_factory=list)
    output_urls: List[str] = Field(default_factory=list)
    created_at: str
    updated_at: str
    debug: Dict[str, object] = Field(default_factory=dict)


class NarratoTaskResponse(ApiResponse):
    data: NarratoTaskResponseData
