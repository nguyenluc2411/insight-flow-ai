FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY target/sales-service-*.jar app.jar

HEALTHCHECK --interval=30s --timeout=10s --start-period=120s --retries=3 \
  CMD wget -qO /dev/null http://localhost:8083/actuator/health || exit 1

EXPOSE 8083

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Duser.timezone=UTC", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
