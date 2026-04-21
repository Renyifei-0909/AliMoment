from __future__ import annotations

import math
from dataclasses import dataclass
from typing import Any, Dict, List, Optional

from app.pcnet_service.service import get_pcnet_service
from app.services.asset_service import DemoAsset, get_asset_service
from app.services.query_normalizer import QueryNormalizationResult, get_query_normalizer


@dataclass
class SearchResult:
    asset: DemoAsset
    normalization: QueryNormalizationResult
    results: List[Dict[str, Any]]


class SearchService:
    def search(self, *, asset_id: str, query: str, top_k: Optional[int] = None) -> SearchResult:
        asset = get_asset_service().get_asset(asset_id)
        normalization = get_query_normalizer().normalize(query, asset)

        pcnet_result = get_pcnet_service().infer(
            video_id=asset.source_video_id,
            duration=asset.duration,
            query=normalization.translated_query,
            top_k=top_k,
        )
        ranked_results = self._format_results(pcnet_result.proposals)
        return SearchResult(asset=asset, normalization=normalization, results=ranked_results)

    @staticmethod
    def _format_results(proposals: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        losses = [float(proposal["reconstruction_loss"]) for proposal in proposals]
        scores = SearchService._losses_to_scores(losses)

        results: List[Dict[str, Any]] = []
        for proposal, score in zip(proposals, scores):
            results.append(
                {
                    "start_time": float(proposal["start"]),
                    "end_time": float(proposal["end"]),
                    "score": round(score, 4),
                    "rank": int(proposal["rank"]),
                }
            )
        return results

    @staticmethod
    def _losses_to_scores(losses: List[float]) -> List[float]:
        if not losses:
            return []
        weights = [math.exp(-loss) for loss in losses]
        total = sum(weights) or 1.0
        return [weight / total for weight in weights]


_search_service: Optional[SearchService] = None


def get_search_service() -> SearchService:
    global _search_service
    if _search_service is None:
        _search_service = SearchService()
    return _search_service
