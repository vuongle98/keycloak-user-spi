package org.vuong.keycloak.spi;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.ext.AdminRealmResourceProvider;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.vuong.keycloak.spi.resource.AdminUserResource;


public class CustomAdminResourceProvider implements AdminRealmResourceProvider {

    private final KeycloakSession session;

    public CustomAdminResourceProvider(KeycloakSession session) {
        this.session = session;
    }


    @Override
    public void close() {

    }

    @Override
    public Object getResource(
            KeycloakSession keycloakSession,
            RealmModel realmModel,
            AdminPermissionEvaluator adminPermissionEvaluator,
            AdminEventBuilder adminEventBuilder
    ) {
        return new AdminUserResource(session, realmModel, adminPermissionEvaluator, adminEventBuilder);
    }
}
