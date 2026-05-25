# notification-service-master-plan.md

# Notification Service — Enterprise Master Plan

## 1. Service Overview

The `notification-service` is responsible for realtime and asynchronous communication across the Insight Flow AI platform.

Main responsibilities:
- consume Kafka business events
- generate operational alerts
- persist notification inbox
- send websocket realtime notifications
- send email notifications
- manage retry workflows
- support DLQ handling
- provide realtime dashboard synchronization

The service follows:
- Event-Driven Architecture
- Distributed System principles
- Inbox Notification Architecture
- Realtime WebSocket Architecture
- Kafka-based asynchronous processing

---

# 2. System Context

```text
fashion-ai-system/
│
├── 00-infrastructure/
├── 01-platform-services/
├── 02-shared-core/
├── 03-core-business/
├── 04-intelligence/
└── 05-engagement/
    ├── recommendation-service/
    ├── notification-service/
    └── dashboard-service/
```

notification-service belongs to:
- engagement layer
- realtime communication layer
- operational alert layer

---

# 3. Final Architecture Decision

## WebSocket Placement

WebSocket remains INSIDE:
```text
notification-service
```

Reason:
- project scope is moderate
- easier deployment
- easier maintenance
- websocket traffic not massive
- avoids unnecessary microservice complexity

Future scaling can separate:
```text
websocket-gateway-service
```

without rewriting business logic because channel abstraction already exists.

---

# 4. High-Level Architecture

```text
Business Services
        ↓
Kafka Events
        ↓
Kafka Consumers
        ↓
Notification Orchestrator
        ├── Validation
        ├── Idempotency Check
        ├── Aggregation Engine
        ├── Template Engine
        ├── Inbox Persistence
        ├── Channel Routing
        ├── Retry Routing
        ├── Delivery Tracking
        ├── Metrics
        └── Redis Realtime Layer
                ↓
Redis Pub/Sub
                ↓
WebSocket Delivery
                ↓
React Dashboard
```

---

# 5. Final Project Structure

```text
notification-service/
│
├── src/main/java/com/insightflow/notification/
│
│   ├── controller/
│   │
│   ├── service/
│   │   ├── interfaces/
│   │   ├── impl/
│   │   ├── orchestrator/
│   │   ├── retry/
│   │   ├── inbox/
│   │   ├── websocket/
│   │   ├── email/
│   │   ├── redis/
│   │   ├── aggregation/
│   │   └── template/
│   │
│   ├── repository/
│   │
│   ├── dto/
│   │   ├── request/
│   │   ├── response/
│   │   ├── websocket/
│   │   └── kafka/
│   │
│   ├── mapper/
│   │
│   ├── entity/
│   │
│   ├── enums/
│   │
│   ├── config/
│   │   ├── kafka/
│   │   ├── redis/
│   │   ├── websocket/
│   │   ├── security/
│   │   └── swagger/
│   │
│   ├── exception/
│   │
│   ├── consumer/
│   │   ├── trend/
│   │   ├── inventory/
│   │   ├── recommendation/
│   │   └── retry/
│   │
│   ├── producer/
│   │
│   ├── event/
│   │   ├── incoming/
│   │   └── outgoing/
│   │
│   ├── websocket/
│   │   ├── gateway/
│   │   ├── handler/
│   │   └── payload/
│   │
│   ├── redis/
│   │   ├── cache/
│   │   ├── pubsub/
│   │   └── keys/
│   │
│   ├── scheduler/
│   │
│   ├── provider/
│   │   ├── email/
│   │   ├── push/
│   │   └── sms/
│   │
│   ├── util/
│   │
│   └── NotificationServiceApplication.java
│
├── src/main/resources/
│   ├── db/migration/
│   ├── templates/
│   │   ├── email/
│   │   └── notification/
│   │
│   ├── application.yml
│   └── application-dev.yml
│
├── Dockerfile
├── .env
└── pom.xml
```

---

# 6. Infrastructure Separation

## Global Infrastructure

Located at:
```text
00-infrastructure/
```

Contains:
- PostgreSQL
- Kafka
- Zookeeper
- Redis
- Grafana
- Prometheus
- Docker Compose

This is SYSTEM infrastructure.

---

## Service Infrastructure Layer

Located at:
```text
notification-service/config/
notification-service/consumer/
notification-service/provider/
notification-service/websocket/
notification-service/redis/
```

Contains:
- Kafka integration
- Redis integration
- WebSocket setup
- Email providers
- Retry schedulers
- Metrics integrations

This is SERVICE technical infrastructure.

---

# 7. Notification Workflow

```text
Kafka Event
    ↓
Consumer Validation
    ↓
Idempotency Check
    ↓
Notification Orchestrator
    ↓
Priority Evaluation
    ↓
Aggregation Engine
    ↓
Template Rendering
    ↓
Inbox Persistence
    ↓
Channel Routing
    ├── WebSocket
    ├── Email
    └── Push (Future)
    ↓
Delivery Tracking
    ↓
Retry / DLQ
```

---

# 8. Kafka Architecture

## Topics

```text
notifications.high
notifications.normal
notifications.low
notifications.retry
notifications.dlq
```

---

## Retry Topics

```text
notifications.retry.30s
notifications.retry.2m
notifications.retry.10m
```

Benefits:
- scalable
- distributed
- non-blocking
- production-grade retry handling

---

## Incoming Events

Consumed from:
- recommendation-service
- inventory-service
- ai-prediction-service
- dashboard-service

---

# 9. Redis Architecture

Redis responsibilities:
- unread count cache
- websocket session cache
- online presence
- pub/sub
- rate limiting

---

## Redis Keys

```text
notification:unread:{userId}

notification:online:{userId}

notification:rate:{userId}

notification:ws:session:{sessionId}
```

---

# 10. Notification Inbox Model

The service uses a Facebook-style inbox notification model.

Features:
- unread notifications
- mark as read
- archive
- soft delete
- realtime synchronization
- pagination support

---

## Inbox APIs

```text
GET    /api/v1/notifications/inbox

PATCH /api/v1/notifications/{id}/read

PATCH /api/v1/notifications/read-all

DELETE /api/v1/notifications/{id}

GET    /api/v1/notifications/unread-count
```

---

# 11. Channel Architecture

## Supported Channels

Current:
- websocket
- email

Future:
- push notification
- sms

---

## Channel Abstraction

```java
NotificationChannelHandler
```

Purpose:
- loose coupling
- provider replacement
- future scalability
- gateway separation readiness

---

# 12. Reliability Strategy

## Idempotency

Prevent duplicate processing using:
- processed_events table
- eventId tracking
- correlationId tracking

---

## Retry Handling

Temporary failures:
- SMTP timeout
- websocket timeout
- network instability

Permanent failures:
- invalid payload
- invalid recipient

---

## DLQ

Used for:
- replay
- debugging
- failure analysis
- operational visibility

---

# 13. Observability

## Metrics

```text
notification_sent_total

notification_failed_total

notification_retry_total

notification_latency_ms

websocket_connections

notification_dlq_size
```

---

## Monitoring Stack

- Prometheus
- Grafana
- OpenTelemetry

---

# 14. Security

- JWT authentication
- websocket authentication
- role-based notification access
- Redis rate limiting
- secure websocket subscriptions

---

# 15. Recommended Development Phases

## Phase 0 — Infrastructure

Setup:
- PostgreSQL
- Kafka
- Redis
- Eureka
- Flyway
- Docker Compose
- Base Spring Boot config

---

## Phase 1 — Domain Foundation

Build:
- enums
- entities
- repositories
- DTOs
- mappers

---

## Phase 2 — Kafka Event System

Build:
- consumers
- producers
- topic configuration
- retry topics
- DLQ

---

## Phase 3 — Notification Orchestrator

Build:
- orchestration engine
- routing
- aggregation
- template rendering
- delivery tracking

---

## Phase 4 — Redis + WebSocket

Build:
- realtime websocket delivery
- unread cache
- online presence
- Redis pub/sub

---

## Phase 5 — Inbox APIs

Build:
- inbox pagination
- read/unread logic
- archive/delete
- unread counter APIs

---

## Phase 6 — Retry + DLQ

Build:
- retry scheduler
- retry topics
- replay handling
- DLQ consumers

---

## Phase 7 — Observability

Build:
- Prometheus metrics
- Grafana dashboards
- distributed tracing
- alerting

---

# 16. Enterprise Improvements Added Beyond Original Video

## Improvements Added

### 1. Aggregation Engine

Prevent notification spam.

Example:
```text
20 inventory alerts
→ aggregate into 1 notification
```

---

### 2. Kafka Retry Topics

Instead of DB polling retry.

Benefits:
- better scalability
- distributed retries
- non-blocking processing

---

### 3. Redis Presence Layer

Track:
- online users
- active websocket sessions
- multi-device users

---

### 4. Channel Abstraction

Allows:
- provider replacement
- gateway separation
- push notification expansion

---

### 5. Inbox-based Notification Model

Persistent notification center instead of transient websocket-only delivery.

---

### 6. Metrics & Observability

Production-ready monitoring:
- Prometheus
- Grafana
- OpenTelemetry

---

### 7. Future Gateway Separation

Architecture prepared for:
```text
websocket-gateway-service
```

without rewriting business logic.

---

# 17. Final Goal

Build a production-style distributed notification platform that supports:
- realtime operational alerts
- scalable Kafka processing
- Redis realtime optimization
- inbox persistence
- websocket realtime delivery
- retry + DLQ reliability
- observability
- enterprise maintainability
- future scalability