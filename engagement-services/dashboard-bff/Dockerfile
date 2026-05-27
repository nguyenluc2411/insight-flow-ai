FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
WORKDIR /app
USER appuser

COPY target/dashboard-bff-*.jar app.jar

HEALTHCHECK --interval=30s --timeout=10s --start-period=90s --retries=3 \
  CMD wget -qO /dev/null http://localhost:8090/actuator/health || exit 1

EXPOSE 8090

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
