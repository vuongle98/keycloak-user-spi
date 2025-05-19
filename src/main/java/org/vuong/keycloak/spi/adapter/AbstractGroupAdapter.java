package org.vuong.keycloak.spi.adapter;

import org.keycloak.models.ClientModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.storage.StorageId;

import java.util.*;
import java.util.stream.Stream;

public abstract class AbstractGroupAdapter implements GroupModel {
    protected final RealmModel realm;
    protected final String id;
    protected String name;
    protected GroupModel parent;
    protected Set<RoleModel> roles = new HashSet<>();

    public AbstractGroupAdapter(RealmModel realm, String id, String name) {
        this.realm = realm;
        this.id = id;
        this.name = name;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void setSingleAttribute(String name, String value) {
        // Implement if needed
    }

    @Override
    public void setAttribute(String name, List<String> values) {
        // Implement if needed
    }

    @Override
    public void removeAttribute(String name) {
        // Implement if needed
    }

    @Override
    public String getFirstAttribute(String name) {
        return null; // Implement if needed
    }

    @Override
    public Stream<String> getAttributeStream(String name) {
        return Stream.empty(); // Implement if needed
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        return new HashMap<>(); // Implement if needed
    }

    @Override
    public GroupModel getParent() {
        return parent;
    }

    @Override
    public String getParentId() {
        return parent != null ? parent.getId() : null;
    }

    @Override
    public Stream<GroupModel> getSubGroupsStream() {
        return Stream.empty(); // Implement if needed
    }

    @Override
    public void setParent(GroupModel group) {
        this.parent = group;
    }

    @Override
    public void addChild(GroupModel subGroup) {
        // Implement if needed
    }

    @Override
    public void removeChild(GroupModel subGroup) {
        // Implement if needed
    }

    @Override
    public Stream<RoleModel> getRealmRoleMappingsStream() {
        return roles.stream();
    }

    @Override
    public Stream<RoleModel> getClientRoleMappingsStream(ClientModel app) {
        return Stream.empty(); // Implement if needed
    }

    @Override
    public boolean hasRole(RoleModel role) {
        return roles.contains(role);
    }

    @Override
    public void grantRole(RoleModel role) {
        roles.add(role);
    }

    @Override
    public Stream<RoleModel> getRoleMappingsStream() {
        return getRealmRoleMappingsStream();
    }

    @Override
    public void deleteRoleMapping(RoleModel role) {
        roles.remove(role);
    }
}