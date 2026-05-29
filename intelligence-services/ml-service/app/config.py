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

    class Config:
        env_file = ".env"


settings = Settings()
