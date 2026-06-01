"""Model storage: local filesystem cache + MinIO persistent backend.

Strategy:
  - save: write local first, then upload to MinIO (best-effort).
  - load: serve from local cache, download from MinIO on a miss.
  - exists: local OR MinIO.

Local cache avoids a MinIO round-trip on every prediction; after a container
restart the cache is empty and models are re-downloaded from MinIO on demand.

``key`` is always RELATIVE to ``settings.MODEL_STORAGE_PATH`` and is reused
verbatim as the MinIO object key, e.g. "base/ao_so_mi.pkl" or
"{tenant_id}/{variant_id}/{version}.pkl".
"""
from __future__ import annotations

import logging
import pickle
from pathlib import Path
from typing import Any

import boto3
from botocore.exceptions import ClientError

from app.config import settings

logger = logging.getLogger(__name__)


def _local_path(key: str) -> Path:
    return Path(settings.MODEL_STORAGE_PATH) / key


def _get_s3():
    return boto3.client(
        "s3",
        endpoint_url=settings.MINIO_ENDPOINT,
        aws_access_key_id=settings.MINIO_ACCESS_KEY,
        aws_secret_access_key=settings.MINIO_SECRET_KEY,
    )


def save_model(model: Any, key: str) -> None:
    """Persist a picklable model to the local cache and (best-effort) MinIO."""
    local = _local_path(key)
    local.parent.mkdir(parents=True, exist_ok=True)
    with local.open("wb") as f:
        pickle.dump(model, f)
    logger.info("Model saved locally: %s", local)

    if settings.MINIO_ENABLED:
        try:
            _get_s3().upload_file(str(local), settings.MINIO_BUCKET, key)
            logger.info("Model uploaded to MinIO: %s", key)
        except Exception as exc:  # noqa: BLE001 — MinIO failure must not break training
            logger.warning("MinIO upload failed (local copy still valid): %s", exc)


def load_model(key: str) -> Any:
    """Load a model from local cache, downloading from MinIO on a cache miss."""
    local = _local_path(key)
    if local.exists():
        with local.open("rb") as f:
            return pickle.load(f)

    if settings.MINIO_ENABLED:
        try:
            logger.info("Cache miss, downloading from MinIO: %s", key)
            local.parent.mkdir(parents=True, exist_ok=True)
            _get_s3().download_file(settings.MINIO_BUCKET, key, str(local))
            with local.open("rb") as f:
                return pickle.load(f)
        except ClientError as exc:
            if exc.response.get("Error", {}).get("Code") in ("404", "NoSuchKey"):
                raise FileNotFoundError(f"Model not found: {key}") from exc
            raise

    raise FileNotFoundError(f"Model not found locally and MinIO disabled: {key}")


def model_exists(key: str) -> bool:
    """Whether a model exists in the local cache or in MinIO."""
    if _local_path(key).exists():
        return True
    if settings.MINIO_ENABLED:
        try:
            _get_s3().head_object(Bucket=settings.MINIO_BUCKET, Key=key)
            return True
        except ClientError:
            return False
    return False


def list_keys(prefix: str) -> list[str]:
    """List model object keys under a prefix, merging local cache + MinIO.

    Used to discover the latest per-variant model version even when the local
    cache is empty (e.g. right after a container restart).
    """
    keys: set[str] = set()
    base = _local_path(prefix)
    if base.exists():
        for p in base.glob("*.pkl"):
            keys.add(f"{prefix.rstrip('/')}/{p.name}")
    if settings.MINIO_ENABLED:
        try:
            resp = _get_s3().list_objects_v2(Bucket=settings.MINIO_BUCKET, Prefix=prefix)
            for obj in resp.get("Contents", []):
                if obj["Key"].endswith(".pkl"):
                    keys.add(obj["Key"])
        except ClientError as exc:
            logger.warning("MinIO list failed for prefix %s: %s", prefix, exc)
    return sorted(keys)
