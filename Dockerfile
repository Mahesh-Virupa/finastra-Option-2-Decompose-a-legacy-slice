# ---- build (no local JDK/Maven needed) ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY . .
RUN mvn -B -q -DskipTests package

# ---- runtime: small, non-root, runs anywhere a container runs ----
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app && mkdir -p /out && chown app:app /out
WORKDIR /app
COPY --from=build /src/target/option2-trade-slice.jar app.jar
ENV ADAPTER_FILE_DIR=/out JAVA_OPTS="-XX:MaxRAMPercentage=75"
USER app
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --start-period=30s CMD wget -qO- http://localhost:8080/actuator/health/liveness || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
