FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY target/catalog-service-*.jar app.jar

HEALTHCHECK --interval=30s --timeout=10s --start-period=120s --retries=3 \
  CMD wget -qO /dev/null http://localhost:8082/actuator/health || exit 1

EXPOSE 8082

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
