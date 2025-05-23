# Keycloak Custom User Storage Provider (Docker Deployment)

This guide explains how to build and run your custom Keycloak service with a user storage provider using Docker.

## Prerequisites

- Docker installed on your system
- PostgreSQL database accessible to Keycloak

## Build the Provider JAR

First, build your provider JAR and JDBC driver:

```bash
mvn clean package
```

Ensure the following files are present:
- `build/libs/keycloak-user-spi-1.0-SNAPSHOT.jar`
- `build/libs/jdbc/postgresql-<version>.jar`

## Dockerfile Example

Your `Dockerfile` should look like:

```dockerfile
FROM quay.io/keycloak/keycloak:24.0.3

COPY build/libs/keycloak-user-spi-1.0-SNAPSHOT.jar /opt/keycloak/providers/
COPY build/libs/jdbc/postgresql-*.jar /opt/keycloak/providers/

RUN /opt/keycloak/bin/kc.sh build
```

## Build the Docker Image

```bash
docker build -t custom-keycloak:latest .
```

## Run Keycloak with Your Provider

Replace environment variables as needed:

```bash
docker run -d \
  --name my-keycloak \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  -p 8080:8080 \
  custom-keycloak:latest start-dev
```

## Configure the Provider in Keycloak

1. Access the Keycloak admin console at [http://localhost:8080](http://localhost:8080).
2. Log in with the admin credentials.
3. Go to **User Federation**.
4. Add your custom provider and configure the PostgreSQL connection settings.

## Usage

- Manage users, groups, and roles via the Keycloak admin console.
- Changes will be reflected in your external PostgreSQL database.
- Use the "Synchronize" option as needed.

---
**Note:** Restart the container if you update the provider JAR.