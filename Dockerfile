# simple dockerfile for the task management API
FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

# copy build scripts first to leverage cache
COPY pom.xml mvnw ./
COPY .mvn .mvn

COPY src src

RUN chmod +x mvnw && ./mvnw clean package -DskipTests

EXPOSE 8080

ENTRYPOINT ["java","-jar","target/taskmanagement-api-1.0.0.jar"]
