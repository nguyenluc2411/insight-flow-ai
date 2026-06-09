# Sử dụng môi trường Java 21 siêu nhẹ
FROM eclipse-temurin:21-jre-alpine

# Thư mục làm việc bên trong container
WORKDIR /app

# Copy file code đã được build (file .jar) từ máy tính vào container
COPY target/data-ingestion-service-0.0.1-SNAPSHOT.jar app.jar

# Khai báo cổng mạng mà module này sử dụng
EXPOSE 8088

# Lệnh khởi chạy ứng dụng khi container bật lên
ENTRYPOINT ["java", "-jar", "app.jar"]