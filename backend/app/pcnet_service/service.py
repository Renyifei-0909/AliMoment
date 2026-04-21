from __future__ import annotations

import importlib
import pickle
import threading
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Optional

import h5py
import nltk
import numpy as np
import torch

from app.config import settings
from app.pcnet_service.bootstrap import ensure_pcnet_importable


class PCNetServiceError(RuntimeError):
    pass


@dataclass
class PCNetInferenceResult:
    video_id: str
    duration: float
    query: str
    dataset: str
    top_prediction: dict[str, Any]
    proposals: list[dict[str, Any]]


class PCNetService:
    def __init__(self) -> None:
        self._lock = threading.Lock()
        self._loaded = False
        self._load_error: Optional[str] = None

        self._pcnet_root: Optional[Path] = None
        self._config_path: Optional[Path] = None
        self._checkpoint_path: Optional[Path] = None
        self._feature_path: Optional[Path] = None

        self._dataset_config: Optional[dict[str, Any]] = None
        self._checkpoint_config: Optional[dict[str, Any]] = None
        self._keep_vocab: Optional[dict[str, int]] = None
        self._vocab: Optional[dict[str, Any]] = None

        self._device: str = settings.pcnet_device
        self._model: Any = None
        self._feature_file: Optional[h5py.File] = None
        self._cal_nll_loss = None

    def describe(self) -> dict[str, Any]:
        return {
            "ready": self._loaded,
            "dataset": self._dataset_name if self._loaded else None,
            "device": self._device,
            "config_path": str(self._config_path) if self._config_path else settings.pcnet_config_path,
            "checkpoint_path": str(self._checkpoint_path) if self._checkpoint_path else settings.pcnet_checkpoint_path,
            "feature_path": str(self._feature_path) if self._feature_path else settings.pcnet_feature_path,
            "message": self._load_error,
        }

    @property
    def _dataset_name(self) -> str:
        if not self._dataset_config:
            raise PCNetServiceError("PC-Net config is not loaded.")
        return str(self._dataset_config["dataset"])

    def load(self, force: bool = False) -> None:
        with self._lock:
            if self._loaded and not force:
                return

            self._load_error = None
            if self._feature_file is not None:
                self._feature_file.close()
                self._feature_file = None

            try:
                self._load_internal()
                self._loaded = True
            except Exception as exc:  # pragma: no cover - surfaced through API
                self._loaded = False
                self._load_error = str(exc)
                raise

    def reload(self) -> dict[str, Any]:
        self.load(force=True)
        return self.describe()

    def infer(
        self,
        *,
        video_id: str,
        duration: float,
        query: str,
        top_k: Optional[int] = None,
        use_vote: Optional[bool] = None,
    ) -> PCNetInferenceResult:
        self.load()

        with self._lock:
            self._reset_random_state()
            assert self._model is not None
            assert self._keep_vocab is not None
            assert self._dataset_config is not None
            assert self._cal_nll_loss is not None

            frames_feat = self._sample_frame_features(self._load_frame_features(video_id))
            words_feat, words_id, weights = self._encode_query(query)

            batch = self._build_batch(frames_feat, words_feat, words_id, weights)
            output = self._model(epoch=0, **batch)

            num_props = int(self._model.num_props)
            proposal_top_k = min(top_k or settings.pcnet_top_k, num_props)
            vote = settings.pcnet_vote if use_vote is None else use_vote

            words_mask = output["words_mask"].unsqueeze(1).expand(1, num_props, -1).contiguous().view(num_props, -1)
            expanded_words_id = output["words_id"].unsqueeze(1).expand(1, num_props, -1).contiguous().view(num_props, -1)

            nll_loss, _ = self._cal_nll_loss(output["words_logit"], expanded_words_id, words_mask)
            order = nll_loss.view(1, num_props).argsort(dim=-1)

            width = output["width"].view(1, num_props).gather(index=order, dim=-1)
            center = output["center"].view(1, num_props).gather(index=order, dim=-1)

            selected_props = torch.stack(
                [
                    torch.clamp(center - width / 2, min=0),
                    torch.clamp(center + width / 2, max=1),
                ],
                dim=-1,
            )[0].detach().cpu().numpy()
            losses = np.take_along_axis(
                nll_loss.view(1, num_props).detach().cpu().numpy(),
                order.detach().cpu().numpy(),
                axis=1,
            )[0]

            if vote:
                winner = self._vote_best_index(selected_props)
            else:
                winner = 0

            proposals: list[dict[str, Any]] = []
            for rank, (window, loss_value) in enumerate(zip(selected_props[:proposal_top_k], losses[:proposal_top_k]), start=1):
                normalized_start = float(window[0])
                normalized_end = float(window[1])
                proposals.append(
                    {
                        "rank": rank,
                        "start": round(normalized_start * duration, 2),
                        "end": round(normalized_end * duration, 2),
                        "normalized_start": round(normalized_start, 6),
                        "normalized_end": round(normalized_end, 6),
                        "reconstruction_loss": round(float(loss_value), 6),
                    }
                )

            winner_window = selected_props[winner]
            top_prediction = {
                "rank": int(winner + 1),
                "start": round(float(winner_window[0]) * duration, 2),
                "end": round(float(winner_window[1]) * duration, 2),
                "normalized_start": round(float(winner_window[0]), 6),
                "normalized_end": round(float(winner_window[1]), 6),
                "reconstruction_loss": round(float(losses[winner]), 6),
            }

            return PCNetInferenceResult(
                video_id=video_id,
                duration=duration,
                query=query,
                dataset=self._dataset_name,
                top_prediction=top_prediction,
                proposals=proposals,
            )

    def _load_internal(self) -> None:
        self._pcnet_root = ensure_pcnet_importable(settings.pcnet_root)
        self._config_path = Path(settings.pcnet_config_path).expanduser().resolve()
        self._checkpoint_path = Path(settings.pcnet_checkpoint_path).expanduser().resolve()

        if not self._config_path.exists():
            raise PCNetServiceError(f"PC-Net config file does not exist: {self._config_path}")
        if not self._checkpoint_path.exists():
            raise PCNetServiceError(f"PC-Net checkpoint does not exist: {self._checkpoint_path}")

        if not torch.cuda.is_available():
            raise PCNetServiceError(
                "CUDA is not available. The current PC-Net implementation uses CUDA-only tensors, "
                "so please deploy this backend on a GPU server."
            )
        if not str(self._device).startswith("cuda"):
            raise PCNetServiceError("PCNET_DEVICE must point to a CUDA device, for example cuda:0.")

        checkpoint = torch.load(self._checkpoint_path, map_location=self._device)
        checkpoint_config = checkpoint.get("config")
        if not checkpoint_config:
            raise PCNetServiceError("Checkpoint does not contain a saved PC-Net config.")

        self._checkpoint_config = checkpoint_config
        self._dataset_config = dict(checkpoint_config["dataset"])
        if settings.pcnet_feature_path:
            self._dataset_config["feature_path"] = settings.pcnet_feature_path

        self._dataset_config["vocab_path"] = str(self._resolve_pcnet_path(self._dataset_config["vocab_path"]))
        self._dataset_config["feature_path"] = str(self._resolve_pcnet_path(self._dataset_config["feature_path"]))

        self._feature_path = Path(self._dataset_config["feature_path"]).expanduser().resolve()
        if not self._feature_path.exists():
            raise PCNetServiceError(f"Feature file does not exist: {self._feature_path}")

        with open(self._dataset_config["vocab_path"], "rb") as vocab_file:
            self._vocab = pickle.load(vocab_file)

        self._keep_vocab = {}
        vocab_limit = int(self._dataset_config["vocab_size"])
        for word, _ in self._vocab["counter"].most_common(vocab_limit):
            self._keep_vocab[word] = len(self._keep_vocab) + 1

        model_module = importlib.import_module("models")
        loss_module = importlib.import_module("models.loss")

        model_config = dict(checkpoint_config["model"]["config"])
        model_config["vocab_size"] = len(self._keep_vocab) + 1
        model_config["max_epoch"] = int(checkpoint_config["train"]["max_num_epochs"])

        model_cls = getattr(model_module, checkpoint_config["model"]["name"])
        self._model = model_cls(model_config).cuda()
        self._model.load_state_dict(checkpoint["model_parameters"])
        self._model.eval()

        self._feature_file = h5py.File(self._feature_path, "r")
        self._cal_nll_loss = getattr(loss_module, "cal_nll_loss")

    def _resolve_pcnet_path(self, raw_path: str) -> Path:
        candidate = Path(raw_path).expanduser()
        if candidate.is_absolute():
            return candidate.resolve()
        if self._pcnet_root is None:
            raise PCNetServiceError("PC-Net root is not initialized.")
        return (self._pcnet_root / candidate).resolve()

    def _load_frame_features(self, video_id: str) -> np.ndarray:
        if self._feature_file is None:
            raise PCNetServiceError("Feature file is not open.")
        if video_id not in self._feature_file:
            raise PCNetServiceError(f"Video id not found in feature file: {video_id}")

        node = self._feature_file[video_id]
        if self._dataset_name == "ActivityNet":
            if "c3d_features" not in node:
                raise PCNetServiceError(f"ActivityNet video {video_id} does not contain c3d_features.")
            feature_array = np.asarray(node["c3d_features"]).astype(np.float32)
        else:
            feature_array = np.asarray(node).astype(np.float32)

        if feature_array.ndim != 2:
            raise PCNetServiceError(f"Unexpected feature shape for video {video_id}: {feature_array.shape}")
        return feature_array

    def _sample_frame_features(self, frames_feat: np.ndarray) -> np.ndarray:
        assert self._dataset_config is not None
        num_clips = int(self._dataset_config["max_num_frames"])
        keep_idx = np.arange(0, num_clips + 1) / num_clips * len(frames_feat)
        keep_idx = np.round(keep_idx).astype(np.int64)
        keep_idx[keep_idx >= len(frames_feat)] = len(frames_feat) - 1

        sampled = []
        for index in range(num_clips):
            start = keep_idx[index]
            end = keep_idx[index + 1]
            if start == end:
                sampled.append(frames_feat[start])
            else:
                sampled.append(frames_feat[start:end].mean(axis=0))
        return np.stack(sampled, axis=0).astype(np.float32)

    def _encode_query(self, query: str) -> tuple[list[np.ndarray], list[int], list[float]]:
        assert self._vocab is not None
        assert self._keep_vocab is not None

        tagged_words = self._tokenize_and_tag(query)
        weights: list[float] = []
        words: list[str] = []
        for word, tag in tagged_words:
            normalized = word.lower()
            if normalized not in self._keep_vocab or normalized not in self._vocab["w2id"]:
                continue
            words.append(normalized)
            weights.append(self._tag_weight(tag))

        if not words:
            raise PCNetServiceError(
                "The query contains no tokens covered by the current PC-Net vocabulary. "
                "Please try a simpler English query closer to the training data."
            )

        words_id = [self._keep_vocab[word] for word in words]
        start_word = words[0]
        words_feat = [self._vocab["id2vec"][self._vocab["w2id"][start_word]].astype(np.float32)]
        words_feat.extend(self._vocab["id2vec"][self._vocab["w2id"][word]].astype(np.float32) for word in words)
        return words_feat, words_id, weights

    def _build_batch(
        self,
        frames_feat: np.ndarray,
        words_feat: list[np.ndarray],
        words_id: list[int],
        weights: list[float],
    ) -> dict[str, torch.Tensor]:
        assert self._dataset_config is not None
        frame_dim = int(self._dataset_config["frame_dim"])
        word_dim = int(self._dataset_config["word_dim"])
        max_num_frames = int(self._dataset_config["max_num_frames"])

        frames_len = np.asarray([min(len(frames_feat), max_num_frames)], dtype=np.int64)
        words_len = np.asarray([len(words_id)], dtype=np.int64)

        frames_batch = np.zeros((1, max_num_frames, frame_dim), dtype=np.float32)
        words_batch = np.zeros((1, len(words_feat), word_dim), dtype=np.float32)
        words_id_batch = np.zeros((1, len(words_id)), dtype=np.int64)
        weights_batch = np.zeros((1, len(weights)), dtype=np.float32)

        frames_batch[0, : len(frames_feat)] = frames_feat
        words_batch[0, : len(words_feat)] = np.stack(words_feat, axis=0)
        words_id_batch[0, : len(words_id)] = np.asarray(words_id, dtype=np.int64)

        exp_weights = np.exp(np.asarray(weights, dtype=np.float32))
        weights_batch[0, : len(weights)] = exp_weights / exp_weights.sum()

        batch = {
            "frames_feat": torch.from_numpy(frames_batch).cuda(),
            "frames_len": torch.from_numpy(frames_len).cuda(),
            "words_feat": torch.from_numpy(words_batch).cuda(),
            "words_id": torch.from_numpy(words_id_batch).cuda(),
            "weights": torch.from_numpy(weights_batch).cuda(),
            "words_len": torch.from_numpy(words_len).cuda(),
        }
        return batch

    def _tokenize_and_tag(self, query: str) -> list[tuple[str, str]]:
        try:
            tokens = nltk.tokenize.word_tokenize(query)
        except LookupError:
            tokens = nltk.tokenize.wordpunct_tokenize(query)

        try:
            return nltk.pos_tag(tokens)
        except LookupError:
            return [(token, "NN") for token in tokens]

    @staticmethod
    def _tag_weight(tag: str) -> float:
        if "NN" in tag:
            return 2.0
        if "VB" in tag:
            return 4.0
        if "JJ" in tag or "RB" in tag:
            return 2.0
        return 1.0

    @staticmethod
    def _vote_best_index(selected_props: np.ndarray) -> int:
        num_props = selected_props.shape[0]
        votes = np.zeros(num_props, dtype=np.float32)
        for i in range(num_props):
            for j in range(num_props):
                votes[i] += PCNetService._segment_iou(selected_props[i], selected_props[j])
        return int(np.argmax(votes))

    @staticmethod
    def _segment_iou(first: np.ndarray, second: np.ndarray) -> float:
        union_start = min(float(first[0]), float(second[0]))
        union_end = max(float(first[1]), float(second[1]))
        inter_start = max(float(first[0]), float(second[0]))
        inter_end = min(float(first[1]), float(second[1]))
        union = max(union_end - union_start, 1e-10)
        inter = max(inter_end - inter_start, 0.0)
        return inter / union

    @staticmethod
    def _reset_random_state() -> None:
        seed = settings.pcnet_seed
        np.random.seed(seed + 1)
        torch.manual_seed(seed + 2)
        torch.cuda.manual_seed(seed + 4)
        torch.cuda.manual_seed_all(seed + 4)


_pcnet_service: Optional[PCNetService] = None


def get_pcnet_service() -> PCNetService:
    global _pcnet_service
    if _pcnet_service is None:
        _pcnet_service = PCNetService()
    return _pcnet_service
