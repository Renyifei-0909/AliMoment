import os
from pathlib import Path

from dotenv import load_dotenv

load_dotenv()


class Settings:
    repo_root: Path = Path(__file__).resolve().parents[2]
    backend_root: Path = repo_root / "backend"

    mysql_host: str = os.getenv("MYSQL_HOST", "127.0.0.1")
    mysql_port: int = int(os.getenv("MYSQL_PORT", "3306"))
    mysql_user: str = os.getenv("MYSQL_USER", "root")
    mysql_password: str = os.getenv("MYSQL_PASSWORD", "")
    mysql_database: str = os.getenv("MYSQL_DATABASE", "alimoment")

    backend_host: str = os.getenv("BACKEND_HOST", "0.0.0.0")
    backend_port: int = int(os.getenv("BACKEND_PORT", "8000"))

    # PC-Net code, configs, checkpoints, and features are expected to live
    # outside this repo and be injected via environment variables.
    pcnet_root: str = os.getenv("PCNET_ROOT", "")
    pcnet_config_path: str = os.getenv("PCNET_CONFIG_PATH", "")
    pcnet_checkpoint_path: str = os.getenv("PCNET_CHECKPOINT_PATH", "")
    pcnet_feature_path: str = os.getenv("PCNET_FEATURE_PATH", "")
    pcnet_device: str = os.getenv("PCNET_DEVICE", "cuda:0")
    pcnet_top_k: int = int(os.getenv("PCNET_TOP_K", "5"))
    pcnet_vote: bool = os.getenv("PCNET_VOTE", "false").lower() in {"1", "true", "yes", "on"}
    pcnet_seed: int = int(os.getenv("PCNET_SEED", "8"))

    demo_assets_manifest_path: str = os.getenv(
        "DEMO_ASSETS_MANIFEST_PATH",
        str(backend_root / "app" / "demo_assets" / "assets.json"),
    )
    storage_root: str = os.getenv("STORAGE_ROOT", str(backend_root / "storage"))
    storage_inputs_dir: str = os.getenv("STORAGE_INPUTS_DIR", str(Path(storage_root) / "inputs"))
    storage_previews_dir: str = os.getenv("STORAGE_PREVIEWS_DIR", str(Path(storage_root) / "previews"))
    storage_features_dir: str = os.getenv("STORAGE_FEATURES_DIR", str(Path(storage_root) / "features"))
    storage_outputs_dir: str = os.getenv("STORAGE_OUTPUTS_DIR", str(Path(storage_root) / "outputs"))
    backend_public_base_url: str = os.getenv("BACKEND_PUBLIC_BASE_URL", "http://127.0.0.1:8000")

    query_normalizer_mode: str = os.getenv("QUERY_NORMALIZER_MODE", "hybrid")
    llm_enabled: bool = os.getenv("LLM_ENABLED", "false").lower() in {"1", "true", "yes", "on"}
    llm_base_url: str = os.getenv("LLM_BASE_URL", "")
    llm_api_key: str = os.getenv("LLM_API_KEY", "")
    llm_model: str = os.getenv("LLM_MODEL", "")
    llm_timeout_seconds: float = float(os.getenv("LLM_TIMEOUT_SECONDS", "10"))

    def ensure_runtime_dirs(self) -> None:
        Path(self.storage_root).mkdir(parents=True, exist_ok=True)
        Path(self.storage_inputs_dir).mkdir(parents=True, exist_ok=True)
        Path(self.storage_previews_dir).mkdir(parents=True, exist_ok=True)
        Path(self.storage_features_dir).mkdir(parents=True, exist_ok=True)
        Path(self.storage_outputs_dir).mkdir(parents=True, exist_ok=True)


settings = Settings()
