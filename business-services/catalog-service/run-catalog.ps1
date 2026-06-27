$env:POSTGRES_USER="change_me"
$env:POSTGRES_PASSWORD="change_me"
$env:KAFKA_BOOTSTRAP="localhost:9092"
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
