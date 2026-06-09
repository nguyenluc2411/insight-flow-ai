from pydantic import Field
from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    PORT: int = 8000

    # --- ÁNH XẠ DATABASE DÙNG CHUNG ---
    DB_HOST: str = Field(default="localhost", alias="POSTGRES_HOST")
    DB_PORT: int = Field(default=5433, alias="POSTGRES_PORT")
    DB_NAME: str = Field(default="insightflow_ml", alias="ML_DB_NAME")
    DB_USERNAME: str = Field(default="postgres", alias="POSTGRES_USER")
    DB_PASSWORD: str = Field(default="postgres", alias="POSTGRES_PASSWORD")
    DB_SCHEMA: str = "ml_service_db"

    # --- ÁNH XẠ KAFKA ---
    KAFKA_BOOTSTRAP: str = Field(default="localhost:9092", alias="KAFKA_BOOTSTRAP_SERVERS")
    KAFKA_GROUP_ID: str = "ml-service-group"

    MODEL_STORAGE_PATH: str = "./models"
    MIN_DATA_POINTS: int = 30

    # --- ÁNH XẠ MINIO CHUNG ---
    MINIO_ENDPOINT: str = "http://localhost:9000"
    MINIO_ACCESS_KEY: str = Field(default="minioadmin", alias="MINIO_ROOT_USER")
    MINIO_SECRET_KEY: str = Field(default="minioadmin", alias="MINIO_ROOT_PASSWORD")
    MINIO_BUCKET: str = "insightflow-bucket"
    MINIO_ENABLED: bool = True

    # --- LLM ---
    GEMINI_API_KEY: str = ""
    GEMINI_API_URL: str = (
        "https://generativelanguage.googleapis.com/v1beta/models/"
        "gemini-2.5-flash:generateContent"
    )
    # Used when the primary model stays overloaded (503/429). Point this at a
    # known-stable model so a busy primary doesn't fail the whole workspace.
    GEMINI_FALLBACK_API_URL: str = (
        "https://generativelanguage.googleapis.com/v1beta/models/"
        "gemini-2.5-flash:generateContent"
    )
    # Bật tool google_search (grounding) để advisor lấy tín hiệu xu hướng thời trang
    # thật/real-time (mức độ quan tâm tìm kiếm kiểu Google Trends, bối cảnh mùa vụ/lễ
    # tết) cho thị trường TP.HCM / Việt Nam — dùng chung GEMINI_API_KEY, không cần key
    # riêng. Lưu ý: Gemini không cho dùng responseMimeType=application/json cùng tools,
    # nên khi bật cái này ta bỏ responseMimeType và dựa vào prompt + _extract_clean_json.
    GEMINI_USE_SEARCH_GROUNDING: bool = True
    DATA_INGESTION_URL: str = "http://localhost:8088"

    class Config:
        # 1. Trỏ đường dẫn đọc file .env gốc ở root dự án (lùi 2 cấp thư mục)
        # Nếu không thấy, nó sẽ fallback về file .env nội bộ (nếu có).
        env_file = ("../../.env", ".env")

        # 2. Quan trọng: Bỏ qua tất cả các biến rác của service khác (SEPAY, MAIL...) để không bị văng lỗi
        extra = "ignore"
settings = Settings()