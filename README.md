# PharmaErp System

PharmaErp is a micro-segment orchestration platform for pharmaceutical companies, designed to automate daily operational tasks. It features a clean web interface backed by modular services that can be independently scaled and deployed across distributed machines.

## Architecture

| Module | Description |
|--------|-------------|
| **Hibernate** | Data-access layer — JPA/Hibernate entities, validation logic, session management |
| **MainApp** | Standalone console runner for quick testing outside the web container |
| **PharmaWebClinetIntellij** | Servlet-based web application (WAR) with JSP views and JWT authentication |

## Tech Stack

- **Java 8** (source compatibility)
- **Gradle 5.6.4** (multi-project build)
- **Hibernate 5.2** / MySQL 5.1 connector
- **JJWT 0.2** for JSON Web Token auth
- **Bootstrap 4** / jQuery frontend
- **JUnit 4.12** for testing

## Prerequisites

- JDK 8 or later
- MySQL 5.x+ with a `PHARMA_ERP` schema

## Getting Started

### Clone

```bash
git clone https://github.com/COG-GTM/PharmaErp.git
cd PharmaErp
```

### Database Setup

Create the MySQL database and configure credentials. Default connection settings are in `Hibernate/src/main/resources/hibernate.cfg.xml`. For production, override credentials via environment variables or system properties.

### Build

```bash
./gradlew build
```

### Run Console App

```bash
./gradlew :MainApp:run
```

### Deploy Web Application

Build the WAR and deploy to any Servlet 3.1+ container (Tomcat, Jetty, etc.):

```bash
./gradlew :PharmaWebClinetIntellij:war
# WAR is at PharmaWebClinetIntellij/build/libs/PharmaWebClinetIntellij-1.0.war
```

## Project Structure

```
PharmaErp/
├── build.gradle                  # Root build config
├── settings.gradle               # Multi-project settings
├── Hibernate/                    # Data layer module
│   ├── src/main/java/
│   │   ├── com/schema/           # JPA entities (User)
│   │   └── com/pharma/erp/       # Validation, Database, Exception packages
│   └── src/main/resources/       # hibernate.cfg.xml
├── MainApp/                      # Console application module
│   └── src/main/java/MainApp/
├── PharmaWebClinetIntellij/      # Web application module
│   ├── src/main/java/com/pharma/erp/
│   │   ├── LoginServlet/         # Login endpoint
│   │   ├── Session/              # JWT token management
│   │   └── Redirect/             # Redirect servlet
│   └── src/main/webapp/          # JSP views, CSS, JS assets
└── gradle/wrapper/               # Gradle wrapper
```

## Contributing

Contributions are welcome! Please open an issue or submit a pull request.  
