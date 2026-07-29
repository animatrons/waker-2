# Multi-stage build: Maven + JDK 25 → JRE 25 runtime
FROM maven:3.9.11-eclipse-temurin-25 AS build

WORKDIR /workspace

COPY pom.xml .
RUN mvn -q -B dependency:go-offline -DskipTests || true

COPY src ./src
RUN mvn -q -B -DskipTests package \
  && cp target/waker-backend-*.jar /workspace/app.jar

FROM eclipse-temurin:25-jre AS runtime

WORKDIR /app

RUN groupadd --system waker && useradd --system --gid waker waker
USER waker

COPY --from=build /workspace/app.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
