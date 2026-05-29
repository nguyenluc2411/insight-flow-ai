# Insight Flow AI - AI Agent Guide

## Source of truth

- Follow `.github/copilot-instructions.md` and `.github/instructions/*.md` for architectural rules and engagement-layer phases.

## Repository layout

- `business-services/`: Spring Boot domain services (`auth-service`, `catalog-service`, `sales-service`).
- `engagement-services/`: engagement layer services (`recommendation-service`, `notification-service`).
- `intelligence-services/ml-service/`: FastAPI ML service.
- `platform-services/`: gateway/config/discovery servers.
- `infrastructure/docker/docker-compose.yml`: local infra (Postgres, Kafka, Eureka, etc.).
- `docs/`: engagement service design docs (required before engagement changes).

## Service boundaries and data flow

- `catalog-service` owns products, locations, inventory levels, and inventory movements.
- `sales-service` owns customers, suppliers, and sales orders.
- `ml-service` consumes `sales.order.completed` + `catalog.inventory.updated` into `ml_service_db` for forecasting and rule-based recommendations.
- Engagement services are event-driven (see `docs/recommendation-service/*` and `docs/notification-service/*`).

## Kafka topics (existing)

- `catalog.inventory.updated` (published by `catalog-service` after inventory movements).
- `sales.order.completed` (published by `sales-service` when an order is completed).

## Local run commands (known)

- `business-services/catalog-service`:
    - `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`
    - Requires Postgres (5433), Kafka (9092), Eureka (8761).
- `business-services/sales-service`:
    - `export $(grep -v '^#' ../../.env | grep -v '^$' | xargs) && export KAFKA_BOOTSTRAP=$KAFKA_BOOTSTRAP_SERVERS`
    - `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev -Dspring-boot.run.jvmArguments=-Duser.timezone=UTC`
- `engagement-services/recommendation-service` (Phase R1 skeleton):
    - `mvn -f .\engagement-services\recommendation-service\pom.xml test`
    - `mvn -f .\engagement-services\recommendation-service\pom.xml spring-boot:run`
- `intelligence-services/ml-service`:
    - `python -m venv .venv`
    - `pip install -r requirements.txt`
    - `uvicorn app.main:app --reload --port 8000`

## API access notes

- Gateway Swagger: `http://localhost:8080/swagger-ui/index.html` (Catalog/Sales).
- Direct Swagger: `http://localhost:8082/swagger-ui.html` (Catalog), `http://localhost:8083/swagger-ui.html` (Sales).
- ML Swagger: `http://localhost:8000/docs`.
- ML endpoints (except health) require `X-Tenant-Id` header.

## Engagement service docs (read before changes)

- Recommendation: `docs/recommendation-service/{business-analysis.md,event-flow.md,database-design.md,api-specification.md}`.
- Notification: `docs/notification-service/notification_service_markdown_docs_structure.md` and `docs/notification-service/notification-service-master-plan.md`.
- Dashboard: `docs/dashboard-service/dashboard_service_architecture_design_md.md`.

