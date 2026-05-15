FROM eclipse-temurin:21-jdk AS builder
WORKDIR /build
COPY ../pom.xml .
COPY ../common-lib ./common-lib
COPY . .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar
EXPOSE 8001
ENTRYPOINT ["java", "-Xmx512m", "-Xms256m", "-jar", "app.jar"]
