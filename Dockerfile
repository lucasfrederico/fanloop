# syntax=docker/dockerfile:1
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -e -B dependency:go-offline
COPY src ./src
RUN mvn -q -e -B clean package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
RUN groupadd -r fanloop && useradd -r -g fanloop fanloop
COPY --from=build /app/target/fanloop-server-*.jar app.jar
USER fanloop
EXPOSE 8080
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
