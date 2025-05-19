package org.vuong.keycloak.spi.adapter;

import org.keycloak.models.*;
import org.vuong.keycloak.spi.entity.Group;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class GroupAdapter implements GroupModel {

    private final Group group;
    private final RealmModel realm;
    private final KeycloakSession session;

    public GroupAdapter(KeycloakSession session, RealmModel realm, Group group) {
        this.group = group;
        this.realm = realm;
        this.session = session;
    }

    @Override
    public String getId() {
        return String.valueOf(group.getId());
    }

    @Override
    public String getName() {
        return group.getName();
    }

    @Override
    public void setName(String name) {
        group.setName(name);
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
    public String getFirstAttribute(String name) {
        return "";
    }

    @Override
    public Stream<String> getAttributeStream(String name) {
        return Stream.empty();
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        return Map.of();
    }

    @Override
    public GroupModel getParent() {
        return null;
    }

    @Override
    public String getParentId() {
        return "";
    }

    @Override
    public Stream<GroupModel> getSubGroupsStream() {
        return Stream.empty();
    }

    @Override
    public void setParent(GroupModel group) {

    }

    @Override
    public void addChild(GroupModel subGroup) {

    }

    @Override
    public void removeChild(GroupModel subGroup) {

    }

    @Override
    public Stream<RoleModel> getRealmRoleMappingsStream() {
        return group.getRoles().stream()
                .map(role -> new RoleAdapter(session, realm, role));
    }

    @Override
    public Stream<RoleModel> getClientRoleMappingsStream(ClientModel app) {
        return Stream.empty();
    }

    @Override
    public boolean hasRole(RoleModel role) {
        return getRoleMappingsStream().anyMatch(r -> r.getId().equalsIgnoreCase(role.getId()));
    }

    @Override
    public void grantRole(RoleModel role) {

    }

    @Override
    public Stream<RoleModel> getRoleMappingsStream() {
        return Stream.empty();
    }

    @Override
    public void deleteRoleMapping(RoleModel role) {

    }
}
