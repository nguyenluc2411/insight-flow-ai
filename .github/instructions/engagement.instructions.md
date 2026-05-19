# Engagement Layer Task Instructions

The Engagement Layer contains 3 independent microservices:

* recommendation-service
* notification-service
* dashboard-service

This instruction file defines:

* required documentation reading flow
* implementation phases
* architecture constraints
* service ownership boundaries
* task execution workflow

---

# REQUIRED DOCUMENTATION FLOW

Before generating ANY code, you MUST:

1. identify the target service
2. identify the requested implementation phase
3. read ALL required documentation files
4. analyze existing implementation patterns
5. only then begin implementation

You MUST analyze and reuse patterns from:

* auth-service
* catalog-service
* sales-service
* ml-service

NEVER skip documentation review before coding.

---

# RECOMMENDATION-SERVICE

Purpose:

* generate intelligent business recommendations
* analyze inventory risk
* transform AI predictions into business actions

Responsibilities:

* consume Kafka events from:

    * sales-service
    * catalog-service
    * ml-service
* generate:

    * restock recommendations
    * markdown recommendations
    * clearance strategies
    * bundle suggestions
* publish recommendation events

---

## REQUIRED DOCUMENTS

Before implementation, MUST READ:

1. docs/recommendation-service/business-analysis.md
2. docs/recommendation-service/event-flow.md
3. docs/recommendation-service/database-design.md
4. docs/recommendation-service/api-specification.md

Do NOT implement recommendation-service without reading these documents first.

---

## IMPLEMENTATION PHASES

### R1 — Project Skeleton

Tasks:

* Spring Boot project setup
* package structure
* Dockerfile
* application.yml
* Kafka config skeleton
* PostgreSQL config
* Redis config
* OpenAPI starter

Deliverables:

* buildable service skeleton
* environment configuration
* Docker support

---

### R2 — Flyway Migrations

Tasks:

* create Flyway migration scripts
* define normalized schema
* create indexes
* define UUID identifiers

Deliverables:

* recommendation tables
* audit tables
* inventory risk tables

---

### R3 — Entities + Repositories

Tasks:

* JPA entities
* repositories
* enums
* auditing support

Deliverables:

* persistence layer
* repository interfaces

---

### R4 — DTOs + Mappers + Services

Tasks:

* request/response DTOs
* MapStruct mappers
* service layer
* recommendation business engine

Rules:

* business logic belongs ONLY in service layer
* no controller business logic

Deliverables:

* DTO layer
* mapper layer
* reusable business services

---

### R5 — Kafka Integration

Tasks:

* Kafka consumers
* Kafka producers
* retry strategy
* DLQ support
* idempotent event handling

Deliverables:

* event-driven communication layer

---

### R6 — REST APIs

Tasks:

* REST controllers
* validation
* pagination
* filtering
* global exception handling
* Swagger documentation

Deliverables:

* production-ready APIs

---

### R7 — Gateway + Integration

Tasks:

* gateway routes
* Swagger aggregation
* docker-compose integration
* CURRENT_STATE update

Deliverables:

* deployable integrated service

---

# NOTIFICATION-SERVICE

Purpose:

* provide real-time notification delivery

Responsibilities:

* consume recommendation events
* send websocket notifications
* send email alerts
* manage notification delivery lifecycle

---

## REQUIRED DOCUMENTS

Before implementation, MUST READ:

1. docs/notification-service/notification_service_markdown_docs_structure.md
2. Identify inside the document:
business analysis section
event flow section
database design section
API specification section
websocket architecture section
retry/failure strategy section

Do NOT implement notification-service without reading these documents first.

---

## IMPLEMENTATION PHASES

### N1 — Project Skeleton

### N2 — Flyway + Database

### N3 — Entities + Repositories

### N4 — WebSocket Infrastructure

### N5 — Kafka Consumers

### N6 — Email + Retry + DLQ

### N7 — APIs + Gateway Integration

Rules:

* notification-service must NEVER generate recommendations
* delivery must remain asynchronous
* websocket/email channels must be abstracted

---

# DASHBOARD-SERVICE

Purpose:

* Backend For Frontend (BFF)
* analytics aggregation layer

Responsibilities:

* aggregate data across services
* provide chart-ready APIs
* provide dashboard analytics
* optimize frontend performance using Redis caching

---

## REQUIRED DOCUMENTS

Before implementation, MUST READ:

1. docs/dashboard/dashboard_service_architecture_design_md.md
2. Identify inside the document:
business analysis
event flow
database design
API specifications
Redis caching strategy
aggregation architecture
realtime update flow


Do NOT implement dashboard-service without reading these documents first.

---

## IMPLEMENTATION PHASES

### D1 — Project Skeleton

### D2 — Aggregation Services

### D3 — Redis Cache Layer

### D4 — Analytics APIs

### D5 — Realtime Dashboard Updates

### D6 — Gateway Integration

Rules:

* dashboard APIs are read-only
* expensive queries must be cached
* avoid heavy synchronous fan-out calls

---

# GLOBAL ARCHITECTURE RULES

Mandatory:

* strict layered architecture
* controller → service → repository
* DTO + Mapper pattern mandatory
* constructor injection only
* validation-first APIs
* global exception handling
* OpenAPI documentation
* Flyway migrations required

Consistency rules:

* preserve naming conventions from existing services
* reuse shared-core utilities
* reuse existing Kafka patterns
* preserve package structure consistency

Kafka rules:

* prefer event-driven communication
* consumers must be idempotent
* use retry + DLQ strategy
* events must be immutable DTOs

Database rules:

* PostgreSQL per microservice
* UUID primary keys
* no cross-service database access
* each service owns its own schema

NEVER:

* expose JPA entities directly
* bypass DTO layer
* duplicate business logic
* introduce inconsistent architecture
* implement future phases prematurely

---

# IMPLEMENTATION WORKFLOW

Before generating code:

1. identify current service
2. identify requested phase
3. read all required documentation
4. analyze existing implementations
5. follow existing repository patterns
6. preserve service boundaries
7. generate ONLY requested phase
8. ensure code is commit-ready

IMPORTANT:
Implement ONLY the requested phase.
Do NOT generate future phases or unrelated modules.
