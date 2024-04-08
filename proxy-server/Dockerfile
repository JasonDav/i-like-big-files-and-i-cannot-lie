FROM maven:3.9.6-eclipse-temurin-22-alpine as build

WORKDIR /app
COPY . /app

RUN --mount=type=cache,target=/root/.m2 mvn -f /app/pom.xml clean package -B

FROM eclipse-temurin:22-alpine

COPY --from=build /app/target/*.jar /app/runner.jar

ENTRYPOINT ["java", "-jar", "/app/runner.jar"]