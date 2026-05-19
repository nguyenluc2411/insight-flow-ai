FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/integration-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8084
ENV JAVA_OPTS="-Duser.timezone=UTC -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
