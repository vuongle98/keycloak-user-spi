package org.vuong.keycloak.spi;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;
import org.vuong.keycloak.spi.resource.UserResource;


public class CustomResourceProvider implements RealmResourceProvider {

    private final KeycloakSession session;

    public CustomResourceProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Object getResource() {
        return new UserResource(session);
    }

    @Override
    public void close() {

    }
}
