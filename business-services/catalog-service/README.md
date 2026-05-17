# catalog-service

Domain service quản lý sản phẩm, tồn kho và địa điểm cho Insight Flow AI.

## Scope

- **Products & Variants**: sản phẩm + biến thể (size, màu)
- **Categories**: phân cấp danh mục (parent-child)
- **Locations**: cửa hàng, kho (type: store | warehouse)
- **Inventory Levels**: số lượng tồn kho theo variant × location
- **Inventory Movements**: log append-only mọi thay đổi tồn kho

## Port & Schema

| Property | Value |
|---|---|
| Port | `8082` |
| DB schema | `catalog_db` |
| Eureka name | `catalog-service` |

## Endpoints

```
GET  /api/v1/catalog/products          list (paginated)
POST /api/v1/catalog/products          create
GET  /api/v1/catalog/products/{id}     get by id
PUT  /api/v1/catalog/products/{id}     update
DELETE /api/v1/catalog/products/{id}   soft delete (status=inactive)

GET  /api/v1/catalog/locations         list active
POST /api/v1/catalog/locations         create

GET  /api/v1/catalog/inventory/variants/{variantId}   levels by variant
POST /api/v1/catalog/inventory/movements              record movement
GET  /api/v1/catalog/inventory/movements/{variantId}  movement history
```

## Kafka Events Published

| Topic | Trigger |
|---|---|
| `catalog.inventory.updated` | POST /inventory/movements |

## Run locally

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Requires: Postgres (5433), Kafka (9092), Eureka (8761) running.
See `infrastructure/docker/docker-compose.yml`.

## Swagger UI

Via Gateway: http://localhost:8080/swagger-ui/index.html → "Catalog Service"
Direct: http://localhost:8082/swagger-ui.html
