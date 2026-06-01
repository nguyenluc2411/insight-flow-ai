Backend development rules for Insight Flow AI:

Technology stack:
- Java 21+
- Spring Boot
- Spring Data JPA
- PostgreSQL
- Kafka
- Redis

Architecture rules:
- Layered architecture strictly enforced
- DTO + Mapper pattern required (MapStruct preferred)
- Constructor injection only (no field injection)
- Each service must be independent microservice
- Use Flyway for database migrations

API rules:
- RESTful API design only
- Never expose entities directly
- Use consistent response wrapper format
- Validate all request DTOs using annotations

Error handling:
- Global exception handler required
- Standardized error response format

Performance:
- Use pagination for all list APIs
- Use Redis caching for frequently accessed data
- Avoid heavy synchronous cross-service calls