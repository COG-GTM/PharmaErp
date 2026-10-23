# syntax=docker/dockerfile:1.7
# Multi-stage build: JDK 21 + Gradle wrapper -> slim JRE 21 runtime.
# Build context is backend/ (see the root docker-compose.yml).

FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
# Optional Maven Central mirror (e.g. https://maven-central.storage-download.googleapis.com/maven2)
# for networks where repo.maven.apache.org rate-limits. Empty = use the repositories in the build.
ARG MAVEN_MIRROR=""
RUN if [ -n "$MAVEN_MIRROR" ]; then \
      mkdir -p /root/.gradle/init.d && \
      printf '%s\n' \
        "def mirror = '$MAVEN_MIRROR'" \
        "settingsEvaluated { s -> s.pluginManagement.repositories { maven { url = mirror }; gradlePluginPortal() } }" \
        "allprojects { repositories { maven { url = mirror } } }" \
        > /root/.gradle/init.d/mirror.gradle; \
    fi
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
RUN --mount=type=cache,target=/root/.gradle/caches \
    --mount=type=cache,target=/root/.gradle/wrapper \
    ./gradlew --no-daemon dependencies > /dev/null
COPY src src
# Tests need Docker (Testcontainers) and run in CI; the image build only packages the jar.
RUN --mount=type=cache,target=/root/.gradle/caches \
    --mount=type=cache,target=/root/.gradle/wrapper \
    ./gradlew --no-daemon bootJar -x test && \
    find build/libs -name '*.jar' ! -name '*-plain.jar' -exec cp {} /workspace/app.jar \; && \
    test -s /workspace/app.jar

FROM eclipse-temurin:21-jre
RUN groupadd --system --gid 10001 pharma && useradd --system --uid 10001 --gid pharma --home-dir /app pharma
WORKDIR /app
COPY --from=build --chown=pharma:pharma /workspace/app.jar app.jar
USER pharma
ENV PORT=8090 \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8090
HEALTHCHECK --interval=10s --timeout=5s --start-period=60s --retries=12 \
  CMD curl -fsS "http://127.0.0.1:${PORT}/actuator/health" || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
