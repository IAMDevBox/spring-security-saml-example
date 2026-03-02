FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src src
RUN ./mvnw -q package -DskipTests 2>/dev/null || \
    (apt-get install -y maven 2>/dev/null || apk add maven 2>/dev/null || true) && \
    mvn -q package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8443
ENTRYPOINT ["java", "-jar", "app.jar"]
