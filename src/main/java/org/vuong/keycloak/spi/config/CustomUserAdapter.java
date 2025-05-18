package org.vuong.keycloak.spi.config;

import org.keycloak.models.*;
import org.vuong.keycloak.spi.entity.UserEntity;

import java.util.*;
import java.util.stream.Stream;

public class CustomUserAdapter implements UserModel {

    private final KeycloakSession session;
    private final RealmModel realm;
    private final UserEntity userEntity;

    public CustomUserAdapter(KeycloakSession session, RealmModel realm, UserEntity userEntity) {
        this.session = session;
        this.realm = realm;
        this.userEntity = userEntity;
    }

    @Override
    public String getId() {
        return userEntity.getId().toString();
    }

    @Override
    public String getUsername() {
        return userEntity.getUsername();
    }

    @Override
    public void setUsername(String username) {
        userEntity.setUsername(username);
    }

    @Override
    public Long getCreatedTimestamp() {
        return userEntity.getCreatedAt() != null ? userEntity.getCreatedAt().getTime() : null;
    }

    @Override
    public void setCreatedTimestamp(Long timestamp) {
        // Optional: convert Long to Date and set in entity
    }

    @Override
    public boolean isEnabled() {
        return userEntity.getLocked();
    }

    @Override
    public void setEnabled(boolean enabled) {
        userEntity.setLocked(enabled);
    }

    @Override
    public void setSingleAttribute(String name, String value) {
        // Optional: implement custom attribute handling
    }

    @Override
    public void setAttribute(String name, List<String> values) {
        // Optional: implement custom attribute handling
    }

    @Override
    public void removeAttribute(String name) {
        // Optional: implement custom attribute handling
    }

    @Override
    public String getFirstAttribute(String name) {
        return null;
    }

    @Override
    public Stream<String> getAttributeStream(String name) {
        return Stream.empty();
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        return Collections.emptyMap();
    }

    @Override
    public Stream<String> getRequiredActionsStream() {
        return Stream.empty();
    }

    @Override
    public void addRequiredAction(String action) {
        // Optional
    }

    @Override
    public void removeRequiredAction(String action) {
        // Optional
    }

    @Override
    public String getFirstName() {
        return "default";
    }

    @Override
    public void setFirstName(String firstName) {

    }

    @Override
    public String getLastName() {
        return "default";
    }

    @Override
    public void setLastName(String lastName) {

    }

    @Override
    public String getEmail() {
        return userEntity.getEmail();
    }

    @Override
    public void setEmail(String email) {
        userEntity.setEmail(email);
    }

    @Override
    public boolean isEmailVerified() {
        return false;
    }

    @Override
    public void setEmailVerified(boolean verified) {

    }

    @Override
    public Stream<GroupModel> getGroupsStream() {
        return Stream.empty(); // or load from DB
    }

    @Override
    public void joinGroup(GroupModel group) {
        // Optional
    }

    @Override
    public void leaveGroup(GroupModel group) {
        // Optional
    }

    @Override
    public boolean isMemberOf(GroupModel group) {
        return false;
    }

    @Override
    public String getFederationLink() {
        return null;
    }

    @Override
    public void setFederationLink(String link) {
        // Optional
    }

    @Override
    public String getServiceAccountClientLink() {
        return null;
    }

    @Override
    public void setServiceAccountClientLink(String clientInternalId) {
        // Optional
    }

    @Override
    public SubjectCredentialManager credentialManager() {
//        return new LegacyUserCredentialManager(session, realm, this);
        return null;
    }

    @Override
    public Stream<RoleModel> getRealmRoleMappingsStream() {
        return Stream.empty();
    }

    @Override
    public Stream<RoleModel> getClientRoleMappingsStream(ClientModel app) {
        return Stream.empty();
    }

    @Override
    public boolean hasRole(RoleModel role) {
        return false;
    }

    @Override
    public void grantRole(RoleModel role) {
        // Optional
    }

    @Override
    public Stream<RoleModel> getRoleMappingsStream() {
        return Stream.empty();
    }

    @Override
    public void deleteRoleMapping(RoleModel role) {
        // Optional
    }
}
