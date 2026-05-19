You are working on Insight Flow AI - an enterprise-grade microservices system for fashion trend intelligence and inventory optimization.

This system is event-driven and built using:
- Java Spring Boot microservices
- Kafka for asynchronous communication
- PostgreSQL per service
- Redis for caching
- REST APIs for external communication

Core architecture principles:
- Strict layered architecture (controller → service → repository)
- DTO + Mapper pattern mandatory (no entity exposure in API)
- Constructor injection only
- Validation-first API design
- Global exception handling required
- OpenAPI/Swagger documentation required

Existing services in the system:
- auth-service
- catalog-service
- sales-service
- ml-service
- 05-engagement (recommendation, notification, dashboard)

Engagement layer is the business intelligence layer:
- recommendation-service → generates business actions
- notification-service → sends real-time alerts
- dashboard-service → aggregates data for frontend

Before generating any code:
1. Analyze existing services and patterns
2. Identify similar implementations in catalog-service and sales-service
3. Follow existing structure strictly
4. Use event-driven design when applicable

NEVER:
- expose JPA entities in controllers
- introduce new architecture styles
- bypass DTO layer
- hardcode business logic without service abstraction