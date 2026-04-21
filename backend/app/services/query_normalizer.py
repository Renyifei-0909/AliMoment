from __future__ import annotations

import json
import re
from dataclasses import dataclass
from typing import Dict, Optional
from urllib import error, request

from app.config import settings
from app.services.asset_service import DemoAsset


@dataclass
class QueryNormalizationResult:
    original_query: str
    translated_query: str
    normalization_mode: str


class QueryNormalizer:
    def normalize(self, query: str, asset: DemoAsset) -> QueryNormalizationResult:
        cleaned = self._clean_query(query)
        mode = settings.query_normalizer_mode.lower().strip() or "hybrid"
        alias_hit = self._match_alias(cleaned, asset.query_aliases)
        if alias_hit:
            return QueryNormalizationResult(
                original_query=query,
                translated_query=alias_hit,
                normalization_mode="alias",
            )

        if self._looks_english(cleaned):
            return QueryNormalizationResult(
                original_query=query,
                translated_query=cleaned,
                normalization_mode="passthrough",
            )

        if mode in {"hybrid", "llm"}:
            llm_result = self._normalize_with_llm(cleaned)
            if llm_result:
                return QueryNormalizationResult(
                    original_query=query,
                    translated_query=llm_result,
                    normalization_mode="llm",
                )

        if mode in {"hybrid", "fallback", "alias"} and asset.suggested_queries:
            return QueryNormalizationResult(
                original_query=query,
                translated_query=asset.suggested_queries[0],
                normalization_mode="suggested_query",
            )

        return QueryNormalizationResult(
            original_query=query,
            translated_query=cleaned,
            normalization_mode="fallback",
        )

    @staticmethod
    def _clean_query(query: str) -> str:
        return re.sub(r"\s+", " ", query.strip())

    @staticmethod
    def _match_alias(query: str, aliases: Dict[str, str]) -> Optional[str]:
        normalized_query = query.lower().replace(" ", "")
        for alias, translated in aliases.items():
            if normalized_query == alias.lower().replace(" ", ""):
                return translated
        for alias, translated in aliases.items():
            compact_alias = alias.lower().replace(" ", "")
            if compact_alias and compact_alias in normalized_query:
                return translated
        return None

    @staticmethod
    def _looks_english(query: str) -> bool:
        if not query:
            return False
        ascii_letters = sum(1 for char in query if char.isascii() and char.isalpha())
        return ascii_letters >= max(3, len(query.replace(" ", "")) // 2)

    @staticmethod
    def _normalize_with_llm(query: str) -> Optional[str]:
        if not settings.llm_enabled:
            return None
        if not settings.llm_base_url or not settings.llm_model or not settings.llm_api_key:
            return None

        endpoint = settings.llm_base_url.rstrip("/") + "/chat/completions"
        payload = {
            "model": settings.llm_model,
            "temperature": 0,
            "messages": [
                {
                    "role": "system",
                    "content": (
                        "You normalize Chinese video-retrieval queries into short English phrases. "
                        "Reply with only one concise English phrase."
                    ),
                },
                {
                    "role": "user",
                    "content": query,
                },
            ],
        }
        body = json.dumps(payload).encode("utf-8")
        req = request.Request(
            endpoint,
            data=body,
            headers={
                "Authorization": f"Bearer {settings.llm_api_key}",
                "Content-Type": "application/json",
            },
            method="POST",
        )
        try:
            with request.urlopen(req, timeout=settings.llm_timeout_seconds) as resp:
                raw = json.loads(resp.read().decode("utf-8"))
        except (error.URLError, error.HTTPError, TimeoutError, ValueError, json.JSONDecodeError):
            return None

        choices = raw.get("choices") or []
        if not choices:
            return None
        message = choices[0].get("message") or {}
        content = str(message.get("content", "")).strip()
        if not content:
            return None
        return QueryNormalizer._clean_query(content.splitlines()[0])


_query_normalizer: Optional[QueryNormalizer] = None


def get_query_normalizer() -> QueryNormalizer:
    global _query_normalizer
    if _query_normalizer is None:
        _query_normalizer = QueryNormalizer()
    return _query_normalizer
