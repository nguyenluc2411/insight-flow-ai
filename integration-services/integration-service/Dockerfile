FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
WORKDIR /app
USER appuser

COPY target/integration-service-*.jar app.jar

HEALTHCHECK --interval=30s --timeout=10s --start-period=120s --retries=3 \
  CMD wget -qO /dev/null http://localhost:8084/actuator/health || exit 1

EXPOSE 8084

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Duser.timezone=UTC", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
