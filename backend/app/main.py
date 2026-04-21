from fastapi import Depends, FastAPI
from sqlalchemy import text
from sqlalchemy.orm import Session

from app.database import get_db
from app.pcnet_service.router import router as pcnet_router

app = FastAPI(title="Alimoment API", version="0.1.0")
app.include_router(pcnet_router)


@app.get("/health")
def health():
    return {"status": "ok"}


@app.get("/db-health")
def db_health(db: Session = Depends(get_db)):
    db.execute(text("SELECT 1"))
    return {"database": "ok"}
