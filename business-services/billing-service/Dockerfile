FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/billing-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8086
ENTRYPOINT ["java", "-Duser.timezone=Asia/Ho_Chi_Minh", "-jar", "app.jar"]
