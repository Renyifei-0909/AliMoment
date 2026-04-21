from pathlib import Path

from fastapi import Depends, FastAPI
from fastapi.staticfiles import StaticFiles
from sqlalchemy import text
from sqlalchemy.orm import Session

from app.api.routes_assets import router as assets_router
from app.api.routes_search import router as search_router
from app.config import settings
from app.database import get_db
from app.pcnet_service.router import router as pcnet_router

app = FastAPI(title="Alimoment API", version="0.1.0")
settings.ensure_runtime_dirs()
app.include_router(pcnet_router)
app.include_router(assets_router)
app.include_router(search_router)
app.mount("/media/outputs", StaticFiles(directory=Path(settings.storage_outputs_dir), check_dir=False), name="media-outputs")


@app.get("/health")
def health():
    return {"status": "ok"}


@app.get("/db-health")
def db_health(db: Session = Depends(get_db)):
    db.execute(text("SELECT 1"))
    return {"database": "ok"}
