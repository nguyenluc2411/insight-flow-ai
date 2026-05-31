# ml-service

Demand forecasting (Prophet) and rule-based inventory recommendations for Insight Flow AI.

## Quick start

```bash
cd intelligence-services/ml-service
python -m venv .venv
source .venv/Scripts/activate    # Windows Git Bash
pip install -r requirements.txt
cp .env.example .env
uvicorn app.main:app --reload --port 8000
```

Open Swagger at: http://localhost:8000/docs

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET    | `/api/v1/ml/health` | Service + Kafka + DB status |
| GET    | `/api/v1/ml/forecast/{variant_id}?days=30` | 30-day forecast for one variant |
| POST   | `/api/v1/ml/forecast/batch` | Batch forecast for multiple variants |
| GET    | `/api/v1/ml/recommendations` | Paginated recommendations (action/priority filter) |
| POST   | `/api/v1/ml/recommendations/refresh` | Trigger async refresh job |

All endpoints (except health) require `X-Tenant-Id` header.

## Architecture

- **FastAPI** serves prediction endpoints.
- **Kafka consumer** ingests `sales.order.completed` and `catalog.inventory.updated` events into a local `ml_service_db` schema for offline feature engineering.
- **Prophet** is used per (tenant, variant) for forecasting. Models are stored as pickles under `MODEL_STORAGE_PATH/{tenant_id}/{variant_id}/{version}.pkl`. **Not committed to Git.**
- **Cold-start fallback**: when no per-variant model is available, a 30-day moving average is used with `confidence=low`.
- **Recommendation engine**: rule-based (CLEARANCE / RESTOCK / PROMOTE / OK).

## Tenant isolation

Every persisted row carries `tenant_id`. Every query filters by it. Model files are stored under per-tenant directories.

## Configuration

Override via env vars (see `.env.example`).

## Through the gateway

Once the gateway route is registered (`api-gateway/application.yml` adds an `http://` route for `/api/v1/ml/**`), endpoints are reachable at `http://localhost:8080/api/v1/ml/...`.
