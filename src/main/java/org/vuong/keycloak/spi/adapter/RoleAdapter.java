package org.vuong.keycloak.spi.adapter;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.vuong.keycloak.spi.entity.Role;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class RoleAdapter implements RoleModel {

    private final KeycloakSession session;
    private final Role role;
    private final RealmModel realm;


    public RoleAdapter(KeycloakSession session, RealmModel realm,  Role role) {
        this.session = session;
        this.role = role;
        this.realm = realm;
    }

    @Override
    public String getName() {
        return role.getName();
    }

    @Override
    public String getDescription() {
        return role.getDescription();
    }

    @Override
    public void setDescription(String description) {
        role.setDescription(description);
    }

    @Override
    public String getId() {
        return role.getId().toString();
    }

    @Override
    public void setName(String name) {
        role.setName(name);
    }

    @Override
    public boolean isComposite() {
        return false;
    }

    @Override
    public void addCompositeRole(RoleModel role) {

    }

    @Override
    public void removeCompositeRole(RoleModel role) {

    }

    @Override
    public Stream<RoleModel> getCompositesStream(String search, Integer first, Integer max) {
        return Stream.empty();
    }

    @Override
    public boolean isClientRole() {
        return false;
    }

    @Override
    public String getContainerId() {
        return "";
    }

    @Override
    public RoleContainerModel getContainer() {
        return realm;
    }

    @Override
    public boolean hasRole(RoleModel role) {
        return false;
    }

    @Override
    public void setSingleAttribute(String name, String value) {

    }

    @Override
    public void setAttribute(String name, List<String> values) {

    }

    @Override
    public void removeAttribute(String name) {

    }

    @Override
    public Stream<String> getAttributeStream(String name) {
        return Stream.empty();
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        return Map.of();
    }
}
