# Legacy PharmaErp (unchanged source) packaged for side-by-side parity runs.
# Build context: repository root. Adapted from the approach in PR #3.
# Gradle 4.4.1 only runs on Java 8; Hibernate 5.2 needs JAXB, so the runtime is Java 8 too.
FROM eclipse-temurin:8-jdk AS build
WORKDIR /workspace
COPY . .
RUN chmod +x gradlew
# Maven Central rate-limits CI/VM egress (HTTP 429); resolve through Google's Maven Central mirror instead.
RUN mkdir -p /root/.gradle/init.d && printf '%s\n' \
    'allprojects { repositories { maven { url "https://maven-central.storage-download.googleapis.com/maven2" } } }' \
    > /root/.gradle/init.d/mirror.gradle
ARG DB_HOST=legacy-db
RUN sed -i "s#//localhost:3306#//${DB_HOST}:3306#g" Hibernate/src/main/resources/hibernate.cfg.xml
RUN ./gradlew --no-daemon :PharmaWebClinetIntellij:war

FROM tomcat:9.0-jre8
RUN rm -rf /usr/local/tomcat/webapps/*
COPY --from=build /workspace/PharmaWebClinetIntellij/build/libs/PharmaWebClinetIntellij-1.0.war /usr/local/tomcat/webapps/ROOT.war
EXPOSE 8080
CMD ["catalina.sh", "run"]
