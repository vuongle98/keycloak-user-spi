package org.vuong.keycloak.spi.adapter;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.vuong.keycloak.spi.entity.Permission;

public class PermissionAdapter {

    private final KeycloakSession session;
    private final RealmModel realm;
    private final ComponentModel model;
    private final Permission permission;

    public PermissionAdapter(KeycloakSession session, RealmModel realm, ComponentModel model, Permission permission) {
        this.session = session;
        this.realm = realm;
        this.model = model;
        this.permission = permission;
    }

    public String getId() {
        return String.valueOf(permission.getId());
    }

    public String getName() {
        return permission.getName();
    }

    public Permission getEntity() {
        return permission;
    }
}
