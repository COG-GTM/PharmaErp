# PharmErp System

PharmaErp System is a tool with a focus on automation in every working aspect of the pharmaceutical companies having a tough time managing their daily tasks.
PharmaErp comes with a lucid user interface which is backed by the powerful division of services in micro-segment that enable the possibility to change system rapidly and can my hosted on distributed machines which realizes the nature of orchestration.

## Project layout

This is a multi-module Gradle build (registered in the root `settings.gradle`):

| Module | Description |
| --- | --- |
| `Hibernate/` | Persistence layer: Hibernate entities (`com.schema.User`, `com.schema.Student`), validation, and `SessionFactory_Helper`. Owns `hibernate.cfg.xml`. |
| `MainApp/` | Console application (`MainApp.MainApp`) that talks to the database through the `Hibernate` module. |
| `PharmaWebClinetIntellij/` | Servlet/JSP web client (WAR). Depends on `Hibernate`. Entry point `/redirect` forwards to `WEB-INF/jsp/main.jsp`; `/login` issues a JWT. |

### Cloning Repository

To clone the repository:

    git clone https://github.com/COG-GTM/PharmaErp

## Prerequisites

* **JDK 8** — the Gradle wrapper pins Gradle 4.4.1, which only runs on Java 8. Set `JAVA_HOME` to a JDK 8 before building, e.g.

      export JAVA_HOME=/path/to/jdk8

* **MySQL 5.x** — only required when actually running the app (see Configuration). Not needed to compile/build.
* **Docker + Docker Compose** — only required for the containerised workflow.

## Building from source

Build every module and produce the WARs with the Gradle wrapper:

    ./gradlew build

Build artifacts:

* Web client WAR: `PharmaWebClinetIntellij/build/libs/PharmaWebClinetIntellij-1.0.war`
* Root aggregate WAR: `build/libs/Pharma-1.0-SNAPSHOT.war`

To build only the deployable web WAR (and its `Hibernate` dependency):

    ./gradlew :PharmaWebClinetIntellij:war

## Configuration

Database connection settings live in
`Hibernate/src/main/resources/hibernate.cfg.xml` and are baked into the WAR (the
`Hibernate` module jar is bundled under `WEB-INF/lib`). Defaults:

| Setting | Value |
| --- | --- |
| JDBC URL | `jdbc:mysql://localhost:3306/PHARMA_ERP` |
| Driver | `com.mysql.jdbc.Driver` (mysql-connector-java 5.1.6) |
| Dialect | `org.hibernate.dialect.MySQL57Dialect` |
| Username | `root` |
| Password | `admin` |

These are **placeholder/local-development credentials** — change the username,
password and URL for any real deployment. `hbm2ddl.auto` is intentionally left
empty, so the schema is **not** created automatically; use `db/init.sql` (the
Docker workflow applies it for you) or create the `user` and `student` tables
manually.

## Running the console app (MainApp)

`MainApp` connects to MySQL using the settings above, so a reachable database
with the schema from `db/init.sql` must exist first.

    ./gradlew :MainApp:run

## Running the web client locally

After `./gradlew :PharmaWebClinetIntellij:war`, drop the generated WAR into any
Servlet 3.1+ container (Tomcat 9, etc.). Deployed as the ROOT context the app is
served at `http://localhost:8080/` (which redirects to `/redirect` → the login
page). A reachable MySQL instance matching the configuration above is required.

## Deploying with Docker / Docker Compose (recommended)

The repository ships a multi-stage `Dockerfile` (builds the WAR with JDK 8, then
runs it in Tomcat 9) and a `docker-compose.yml` that wires the app to a MySQL
service whose credentials match `hibernate.cfg.xml`.

Bring the full stack up:

    docker compose up --build

This will:

1. Start MySQL 5.7 (`PHARMA_ERP` database, user `root` / password `admin`) and
   initialise the schema from `db/init.sql` on first run.
2. Build the WAR and serve it from Tomcat.

Once both containers are healthy the app is available at:

    http://localhost:8080/

The compose build passes `DB_HOST=db` so the in-container Hibernate config points
at the `db` service instead of `localhost`. The database name and credentials are
the local-development defaults baked into the WAR via `hibernate.cfg.xml`; to use
different values, edit `hibernate.cfg.xml` (and the matching `db/init.sql` /
`docker-compose.yml` settings) and rebuild the image. The MySQL port is published
only on `127.0.0.1`, so the database is reachable from the host (for
`./gradlew :MainApp:run`) but not from other machines.

Tear everything down (and drop the database volume):

    docker compose down -v

## Continuous deployment

`CD.sh` scaffolds the exploded-WAR directory layout used for rapid redeployment.
For an automated build + container deploy, run the Docker workflow above from CI.

## Contributing

If you are looking to contribute to this project you can mail us at tp0265@gmail.com.
