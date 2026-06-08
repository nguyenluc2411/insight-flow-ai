import os
import pickle
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import pandas as pd

app = FastAPI(
    title="Predictive Engine API", 
    description="API cung cấp dự báo chuỗi thời gian cho Java Microservices",
    version="2.0"
)
# Đường dẫn tới thư mục chứa các file .pkl
MODEL_DIR = "./models"
# Dictionary để lưu trữ các mô hình trong bộ nhớ RAM
loaded_models = {}

# Hàm tự động quét và nạp tất cả file .pkl khi khởi động server
@app.on_event("startup")
def load_all_models():
    if not os.path.exists(MODEL_DIR):
        print(f"Thư mục {MODEL_DIR} không tồn tại!")
        return
        
    for filename in os.listdir(MODEL_DIR):
        if filename.endswith(".pkl"):
            # Lấy tên mô hình làm Key (Ví dụ: "prophet_áo_dài.pkl" -> key là "áo_dài")
            model_key = filename.replace("prophet_", "").replace(".pkl", "")
            file_path = os.path.join(MODEL_DIR, filename)
            
            try:
                with open(file_path, "rb") as f:
                    loaded_models[model_key] = pickle.load(f)
                print(f"--> Đã nạp thành công mô hình: [{model_key}] từ {filename}")
            except Exception as e:
                print(f"Lỗi khi nạp file {filename}: {str(e)}")

    print(f"Tổng số mô hình đã sẵn sàng: {len(loaded_models)}")

# Định nghĩa cấu trúc Request mới (Bổ sung thêm trường model_name)
class DynamicForecastRequest(BaseModel):
    model_name: str  # Ví dụ: "áo_dài", "áo_tứ_thân"
    periods: int = 30
    frequency: str = "D"

@app.post("/api/v1/forecast")
def predict_dynamic(request: DynamicForecastRequest):
    # 1. Kiểm tra xem mô hình yêu cầu có tồn tại trong bộ nhớ không
    if request.model_name not in loaded_models:
        raise HTTPException(
            status_code=404, 
            detail=f"Mô hình '{request.model_name}' không tồn tại trên hệ thống."
        )
    
    try:
        # 2. Lấy mô hình chính xác ra từ dictionary
        model = loaded_models[request.model_name]
        
        # 3. Tiến hành dự báo
        future = model.make_future_dataframe(periods=request.periods, freq=request.frequency)
        forecast = model.predict(future)
        
        forecast['ds'] = forecast['ds'].dt.strftime('%Y-%m-%d')
        result = forecast[['ds', 'yhat', 'yhat_lower', 'yhat_upper']].to_dict(orient="records")
        
        return {
            "status": "SUCCESS", 
            "model_used": request.model_name,
            "predictions": result
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))