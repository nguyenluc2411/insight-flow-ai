from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    PORT: int = 8000
    DB_HOST: str = "localhost"
    DB_PORT: int = 5433
    DB_NAME: str = "insightflow_ml"
    DB_USERNAME: str = "postgres"
    DB_PASSWORD: str = "postgres"
    DB_SCHEMA: str = "ml_service_db"
    KAFKA_BOOTSTRAP: str = "localhost:9092"
    KAFKA_GROUP_ID: str = "ml-service-group"
    MODEL_STORAGE_PATH: str = "./models"
    MIN_DATA_POINTS: int = 30

    # MinIO / S3-compatible object storage (persistent model backend).
    # In Docker use "http://minio:9000"; on host override with localhost.
    MINIO_ENDPOINT: str = "http://minio:9000"
    MINIO_ACCESS_KEY: str = "minioadmin"
    MINIO_SECRET_KEY: str = "minioadmin"
    MINIO_BUCKET: str = "ml-models"
    MINIO_ENABLED: bool = True  # False = local-only (dev/test without MinIO)

    # LLM inventory advisor (folded in from the former Java recommendation-service).
    # Triggered by inventory.ingestion.completed (file-upload flow).
    GEMINI_API_KEY: str = ""
    GEMINI_API_URL: str = (
        "https://generativelanguage.googleapis.com/v1beta/models/"
        "gemini-2.5-flash:generateContent"
    )
    # data-ingestion-service — source of the uploaded inventory snapshot.
    DATA_INGESTION_URL: str = "http://localhost:8088"

    class Config:
        env_file = ".env"


settings = Settings()
