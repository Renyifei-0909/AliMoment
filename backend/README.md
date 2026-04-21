# Backend Deployment Guide

## 1. What this backend does

This backend wraps the existing `PC-Net` inference flow as a FastAPI service and now exposes a lightweight business API for demo assets.

Current capability:
- Load a trained `PC-Net` checkpoint
- Read video features from the configured HDF5 file
- Accept `video_id + duration + query`
- Accept `asset_id + query` through `/api/search`
- Normalize Chinese demo queries with an alias/LLM/fallback hybrid strategy
- Return the top predicted temporal window and ranked proposals

Current limitation:
- It does **not** extract features from raw uploaded videos
- It relies on the original `PC-Net` code path, which currently requires CUDA

In other words, this is the right deployment shape if your server already has:
- the `PC-Net` folder
- the trained checkpoint
- the matching HDF5 feature file
- a GPU environment that has already run `PC-Net` successfully

## 2. Backend structure

New code lives in:
- `backend/app/pcnet_service/`
- `backend/app/api/`
- `backend/app/services/`
- `backend/app/demo_assets/`

Files:
- `backend/app/pcnet_service/bootstrap.py`: make `PC-Net` importable
- `backend/app/pcnet_service/service.py`: load model, vocab, features, run inference
- `backend/app/pcnet_service/router.py`: FastAPI routes
- `backend/app/pcnet_service/schemas.py`: request/response models
- `backend/app/services/asset_service.py`: load and validate demo assets
- `backend/app/services/query_normalizer.py`: alias/LLM/fallback query normalization
- `backend/app/services/search_service.py`: business search flow
- `backend/app/api/routes_assets.py`: `GET /api/assets`
- `backend/app/api/routes_search.py`: `POST /api/search`
- `backend/app/demo_assets/assets.json`: lightweight manifest for demo assets

## 3. Prepare the Python environment on the server

From the repo root:

```bash
cd backend
python3 -m venv .venv
source .venv/bin/activate
pip install --upgrade pip
pip install -r requirements.txt
```

Then install the GPU version of PyTorch that matches your CUDA version.

Example only:

```bash
pip install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cu121
```

If your `PC-Net` environment already works, the safest way is to reuse the same PyTorch/CUDA combination instead of guessing.

If `fairseq` is not already installed in that environment, install the same version used by `PC-Net` before:

```bash
pip install fairseq==0.12.2
```

If NLTK resources are missing, run once:

```bash
python -m nltk.downloader punkt averaged_perceptron_tagger
```

## 4. Configure environment variables

Copy the example file:

```bash
cd backend
cp .env.example .env
```

Then edit `.env` and make sure these are correct:

- `PCNET_ROOT`: absolute path to the deployed `PC-Net` folder
- `PCNET_CHECKPOINT_PATH`: absolute path to `model-best.pt`
- `PCNET_FEATURE_PATH`: absolute path to the matching HDF5 feature file
- `PCNET_DEVICE`: usually `cuda:0`
- `DEMO_ASSETS_MANIFEST_PATH`: path to `backend/app/demo_assets/assets.json`
- `STORAGE_ROOT`: backend storage root used for future inputs/outputs
- `BACKEND_PUBLIC_BASE_URL`: base URL for media links

Notes:
- `PCNET_CONFIG_PATH` should point to the matching dataset config, such as ActivityNet
- `PCNET_FEATURE_PATH` overrides the `feature_path` inside the checkpoint config, which is useful on the server
- Keep `LLM_ENABLED=false` if you have not configured a reachable OpenAI-compatible endpoint yet

## 5. Demo assets manifest

This project now uses a lightweight manifest with:
- `asset_id`: business-facing id used by the frontend
- `source_video_id`: actual video id inside the `ActivityNet` HDF5
- `status`: may stay `metadata_ready` before raw MP4 files are prepared
- `suggested_queries` and `query_aliases`: used by the hybrid query normalizer

This lets you start demo search integration before raw source videos are fully downloaded.

## 6. Start the service

From `backend/`:

```bash
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

If you want auto-reload during development:

```bash
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

## 7. Validate the deployment

Basic backend health:

```bash
curl http://127.0.0.1:8000/health
```

PC-Net health:

```bash
curl http://127.0.0.1:8000/pcnet/health
```

Expected successful shape:

```json
{
  "ready": true,
  "dataset": "ActivityNet",
  "device": "cuda:0",
  "config_path": "...",
  "checkpoint_path": "...",
  "feature_path": "...",
  "message": null
}
```

Reload after changing env or files:

```bash
curl -X POST http://127.0.0.1:8000/pcnet/reload
```

Demo assets API:

```bash
curl http://127.0.0.1:8000/api/assets
```

Business search API:

```bash
curl -X POST http://127.0.0.1:8000/api/search \
  -H "Content-Type: application/json" \
  -d '{
    "asset_id": "demo_activitynet_001",
    "query": "扫落叶"
  }'
```

## 8. Run one inference request

Use a known `video_id` and its duration from your dataset json, for example from `PC-Net/data/activitynet/test_trivial.json`.

Example:

```bash
curl -X POST http://127.0.0.1:8000/pcnet/infer \
  -H "Content-Type: application/json" \
  -d '{
    "video_id": "v_ogQozSI5V8U",
    "duration": 36.55,
    "query": "We see a hallway with a wooden floor.",
    "top_k": 5
  }'
```

Response shape:

```json
{
  "video_id": "v_ogQozSI5V8U",
  "duration": 36.55,
  "query": "We see a hallway with a wooden floor.",
  "dataset": "ActivityNet",
  "top_prediction": {
    "rank": 1,
    "start": 0.0,
    "end": 7.49,
    "normalized_start": 0.0,
    "normalized_end": 0.204925,
    "reconstruction_loss": 1.234567
  },
  "proposals": [
    {
      "rank": 1,
      "start": 0.0,
      "end": 7.49,
      "normalized_start": 0.0,
      "normalized_end": 0.204925,
      "reconstruction_loss": 1.234567
    }
  ]
}
```

## 9. How this connects to the current frontend

At the moment, the existing frontend pages are mostly UI prototypes:
- JavaFX desktop app is the main usable front-end shell
- smart search and smart editing pages can import local videos
- the AI actions are still placeholder behavior
- there is no existing front-end to backend API integration yet

So the practical order should be:

1. Make sure `backend` can load `PC-Net` and answer `/pcnet/infer`
2. Confirm the server can return stable results for known dataset samples
3. Then connect the JavaFX front-end to these endpoints
4. Only after that, add raw-video feature extraction if you need uploaded custom videos

## 10. Important deployment note

This backend currently serves **feature-based retrieval**, not full raw-video editing.

If your final product needs:
- upload a new user video
- run feature extraction on that video
- then send the extracted feature sequence into `PC-Net`

you still need one more module after this step: a video feature extraction pipeline.

That next module should also be placed under `backend/`, but it is intentionally kept separate from the current `pcnet_service` so the code stays clean.
