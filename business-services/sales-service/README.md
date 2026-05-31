# sales-service

Domain service quản lý đơn hàng, khách hàng và nhà cung cấp cho Insight Flow AI.

## Scope

- **Customers**: hồ sơ khách hàng, RFM segmentation
- **Suppliers**: nhà cung cấp hàng hóa
- **Sales Orders**: đơn hàng (pending → completed)
- **Sales Order Items**: chi tiết dòng hàng (append-only)

## Port & Schema

| Property | Value |
|---|---|
| Port | `8083` |
| DB schema | `sales_db` |
| Eureka name | `sales-service` |

## Endpoints

```
GET  /api/v1/sales/orders              list (paginated)
POST /api/v1/sales/orders              create order
GET  /api/v1/sales/orders/{id}         get by id
POST /api/v1/sales/orders/{id}/complete  complete → Kafka event

GET  /api/v1/sales/customers           list (paginated)
POST /api/v1/sales/customers           create
GET  /api/v1/sales/customers/{id}      get by id

GET  /api/v1/sales/suppliers           list
POST /api/v1/sales/suppliers           create
```

## Kafka Events Published

| Topic | Trigger |
|---|---|
| `sales.order.completed` | POST /orders/{id}/complete |

## Run locally

```bash
export $(grep -v '^#' ../../.env | grep -v '^$' | xargs) && export KAFKA_BOOTSTRAP=$KAFKA_BOOTSTRAP_SERVERS
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev \
  -Dspring-boot.run.jvmArguments=-Duser.timezone=UTC
```

## Swagger UI

Via Gateway: http://localhost:8080/swagger-ui/index.html → "Sales Service"
Direct: http://localhost:8083/swagger-ui.html
