import os
from pathlib import Path

from dotenv import load_dotenv

load_dotenv()


class Settings:
    repo_root: Path = Path(__file__).resolve().parents[2]

    mysql_host: str = os.getenv("MYSQL_HOST", "127.0.0.1")
    mysql_port: int = int(os.getenv("MYSQL_PORT", "3306"))
    mysql_user: str = os.getenv("MYSQL_USER", "root")
    mysql_password: str = os.getenv("MYSQL_PASSWORD", "")
    mysql_database: str = os.getenv("MYSQL_DATABASE", "alimoment")

    backend_host: str = os.getenv("BACKEND_HOST", "0.0.0.0")
    backend_port: int = int(os.getenv("BACKEND_PORT", "8000"))

    pcnet_root: str = os.getenv("PCNET_ROOT", str(repo_root / "PC-Net"))
    pcnet_config_path: str = os.getenv(
        "PCNET_CONFIG_PATH",
        str(repo_root / "PC-Net" / "config" / "activitynet" / "main_train_test.json"),
    )
    pcnet_checkpoint_path: str = os.getenv(
        "PCNET_CHECKPOINT_PATH",
        str(repo_root / "PC-Net" / "checkpoints" / "ActivityNet" / "8_2025-05-14_14-07-12" / "model-best.pt"),
    )
    pcnet_feature_path: str = os.getenv("PCNET_FEATURE_PATH", "")
    pcnet_device: str = os.getenv("PCNET_DEVICE", "cuda:0")
    pcnet_top_k: int = int(os.getenv("PCNET_TOP_K", "5"))
    pcnet_vote: bool = os.getenv("PCNET_VOTE", "false").lower() in {"1", "true", "yes", "on"}
    pcnet_seed: int = int(os.getenv("PCNET_SEED", "8"))


settings = Settings()
