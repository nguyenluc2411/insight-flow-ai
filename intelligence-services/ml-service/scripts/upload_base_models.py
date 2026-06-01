"""One-time: upload locally-built base models (+ metadata) to MinIO.

Run after scripts/build_base_model.py so cold-start base models survive a
container restart (downloaded back on cache miss by app.models.storage).

    python scripts/upload_base_models.py
"""
from __future__ import annotations

import sys
from pathlib import Path

# Make the app package importable when run as a standalone script.
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from app.config import settings  # noqa: E402
from app.models.storage import _get_s3  # noqa: E402


def main() -> int:
    base_dir = Path(settings.MODEL_STORAGE_PATH) / "base"
    if not base_dir.exists():
        print(f"No base model directory at {base_dir} — run build_base_model.py first.")
        return 1

    s3 = _get_s3()
    # Ensure the bucket exists (idempotent).
    existing = [b["Name"] for b in s3.list_buckets().get("Buckets", [])]
    if settings.MINIO_BUCKET not in existing:
        s3.create_bucket(Bucket=settings.MINIO_BUCKET)
        print(f"Created bucket: {settings.MINIO_BUCKET}")

    uploaded = 0
    for f in sorted(base_dir.glob("*.pkl")) + sorted(base_dir.glob("*.json")):
        key = f"base/{f.name}"  # relative to MODEL_STORAGE_PATH (storage convention)
        s3.upload_file(str(f), settings.MINIO_BUCKET, key)
        print(f"Uploaded: {key}")
        uploaded += 1

    print(f"Done. Uploaded {uploaded} objects to {settings.MINIO_BUCKET}.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
