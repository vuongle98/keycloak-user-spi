FROM quay.io/keycloak/keycloak:24.0.3

COPY build/libs/keycloak-user-spi-1.0-SNAPSHOT.jar /opt/keycloak/providers/

COPY build/libs/jdbc/postgresql-*.jar /opt/keycloak/providers/

RUN /opt/keycloak/bin/kc.sh build
