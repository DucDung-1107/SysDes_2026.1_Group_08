FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /build

COPY pom.xml .
COPY src ./src

RUN mvn -B package
FROM eclipse-temurin:21-jre-jammy AS runtime

WORKDIR /app
COPY --from=build /build/target/medora-api-0.0.1-SNAPSHOT.jar app.jar
RUN mkdir -p /app/data/attachments && chown -R 10001:10001 /app/data
USER 10001:10001
EXPOSE 8080
ENTRYPOINT [ "java","-jar","app.jar" ]
