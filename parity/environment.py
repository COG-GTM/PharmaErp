#!/usr/bin/env python3
"""Record the versions, images and commit behind a parity run: python3 parity/environment.py > parity/results/environment.json"""
import json, subprocess


def sh(cmd):
    r = subprocess.run(cmd, shell=True, capture_output=True, text=True)
    return (r.stdout or r.stderr).strip()


def first_line(s):
    return next((ln for ln in s.splitlines() if ln and not ln.startswith("Picked up")), "")


env = {
    "git_commit": sh("git rev-parse HEAD"),
    "git_base_branch_commit": sh("git rev-parse origin/devin/hcl-pharmaerp-modernisation"),
    "docker_engine": sh("docker version --format '{{.Server.Version}}'"),
    "docker_compose": sh("docker compose version --short"),
    "legacy": {
        "app_image": sh("docker inspect pharmaerp-legacy-legacy-app-1 --format '{{.Config.Image}} {{.Image}}'"),
        "java": first_line(sh("docker exec pharmaerp-legacy-legacy-app-1 java -version 2>&1")),
        "tomcat": sh("docker exec pharmaerp-legacy-legacy-app-1 sh -c 'ls /usr/local/tomcat/lib/catalina.jar >/dev/null && echo $TOMCAT_VERSION'"),
        "db": sh("docker exec pharmaerp-legacy-legacy-db-1 mysql --version"),
        "stack": "Java 8 servlets + JSP, Hibernate 5.2.12, Gradle 4.4.1, JJWT RS256",
    },
    "migrated": {
        "backend_image": sh("docker inspect pharmaerp-backend-1 --format '{{.Config.Image}} {{.Image}}'"),
        "frontend_image": sh("docker inspect pharmaerp-frontend-1 --format '{{.Config.Image}} {{.Image}}'"),
        "java": first_line(sh("docker exec pharmaerp-backend-1 java -version 2>&1")),
        "spring_boot": sh("grep -oP 'org.springframework.boot\"\\) version \"\\K[^\"]+' backend/build.gradle.kts"),
        "gradle": sh("grep -oP 'gradle-\\K[0-9.]+(?=-bin)' backend/gradle/wrapper/gradle-wrapper.properties"),
        "db": sh("docker exec pharmaerp-mysql-1 mysql --version"),
        "nginx": sh("docker exec pharmaerp-frontend-1 nginx -v 2>&1"),
        "frontend": sh("node -e \"const p=require('./frontend/package.json');const d={...p.dependencies,...p.devDependencies};console.log(['react','vite','typescript','vitest'].map(k=>k+' '+d[k]).join(', '))\""),
        "flyway_migrations": sh("docker exec -e MYSQL_PWD=pharma pharmaerp-mysql-1 mysql -N -upharma PHARMA_ERP -e 'SELECT COUNT(*) FROM flyway_schema_history WHERE success=1'"),
    },
    "host": {"os": sh(". /etc/os-release && echo $PRETTY_NAME"), "python": sh("python3 --version")},
}
print(json.dumps(env, indent=2))
