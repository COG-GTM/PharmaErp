# syntax=docker/dockerfile:1

# ---------------------------------------------------------------------------
# Stage 1 — Build the WAR.
# Gradle 4.4.1 (declared in gradle/wrapper) only runs on Java 8, so the build
# stage uses a JDK 8 image. This produces the web module's WAR, which also
# triggers the build of the :Hibernate module it depends on.
# ---------------------------------------------------------------------------
FROM eclipse-temurin:8-jdk AS build
WORKDIR /workspace

# Copy the whole multi-module project (see .dockerignore for what is excluded).
COPY . .
RUN chmod +x gradlew

# Point Hibernate at the MySQL service defined in docker-compose.
# Locally the config targets localhost; inside the container network the
# database is reachable under the compose service name (defaults to "db").
ARG DB_HOST=db
RUN sed -i "s#//localhost:3306#//${DB_HOST}:3306#g" \
        Hibernate/src/main/resources/hibernate.cfg.xml

# Build the deployable WAR for the web client module.
RUN ./gradlew --no-daemon :PharmaWebClinetIntellij:war

# ---------------------------------------------------------------------------
# Stage 2 — Run the WAR in a servlet container.
# The web module targets the Servlet 3.1 API, which Tomcat 9 implements.
# A Java 8 runtime is required: Hibernate 5.2 relies on JAXB (javax.xml.bind),
# which was removed from the JDK starting with Java 11.
# ---------------------------------------------------------------------------
FROM tomcat:9.0-jre8-temurin
# Deploy the application as the ROOT context so it is served from "/".
RUN rm -rf /usr/local/tomcat/webapps/*
COPY --from=build \
     /workspace/PharmaWebClinetIntellij/build/libs/PharmaWebClinetIntellij-1.0.war \
     /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080
CMD ["catalina.sh", "run"]
