# Build stage
FROM maven:3.9.4-eclipse-temurin-17 AS build
WORKDIR /workspace
# Copy only the files that exist in the repo; do not rely on mvnw/.mvn being present
COPY pom.xml ./
COPY src ./src
RUN mvn -DskipTests package -Dquarkus.package.type=uber-jar

# Runtime stage
FROM eclipse-temurin:17-jre
WORKDIR /app
# Copy the assembled runnable jar produced by Quarkus build (legacy runner)
COPY --from=build /workspace/target/*-runner.jar /app/app.jar
EXPOSE 8082
ENV QUARKUS_HTTP_PORT=8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]