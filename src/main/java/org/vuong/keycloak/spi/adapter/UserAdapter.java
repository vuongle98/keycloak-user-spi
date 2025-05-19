package org.vuong.keycloak.spi.adapter;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.*;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.adapter.AbstractUserAdapterFederatedStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vuong.keycloak.spi.entity.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UserAdapter extends AbstractUserAdapterFederatedStorage {

    private static final Logger log = LoggerFactory.getLogger(UserAdapter.class);
    private final KeycloakSession session;
    private final RealmModel realm;
    private final UserEntity userEntity;
    private final String keycloakId;

    public UserAdapter(KeycloakSession session, RealmModel realm, ComponentModel model, UserEntity userEntity) {
        super(session, realm, model);
        this.session = session;
        this.realm = realm;
        this.userEntity = userEntity;
        this.keycloakId = StorageId.keycloakId(model, getId());
    }

    @Override
    public String getId() {
        return keycloakId;
    }

    public String getPassword() {
        return userEntity.getPassword();
    }

    public void setPassword(String password) {
        userEntity.setPassword(password);
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
        return userEntity.getCreatedAt() != null ? userEntity.getCreatedAt().getEpochSecond() : null;
    }

    @Override
    public void setCreatedTimestamp(Long timestamp) {
        // Optional: convert Long to Date and set in entity
        if (timestamp != null) {
            userEntity.setCreatedAt(Instant.ofEpochSecond(timestamp));
        }
    }

    @Override
    public boolean isEnabled() {
        return userEntity.getLocked() == null || !userEntity.getLocked();
    }

    @Override
    public void setEnabled(boolean enabled) {
        userEntity.setLocked(enabled);
    }

    @Override
    public void setSingleAttribute(String name, String value) {
        // Optional: implement custom attribute handling
        switch (name) {
            case "phone":
                userEntity.getProfile().setPhone(value);
                break;
            case "address":
                userEntity.getProfile().setAddress(value);
                break;
            case "avatar_url":
                userEntity.getProfile().setAvatarUrl(value);
                break;
            default:
                super.setSingleAttribute(name, value);
        }
    }

    @Override
    public void setAttribute(String name, List<String> values) {
        // Optional: implement custom attribute handling
        switch (name) {
            case "phone":
                userEntity.getProfile().setPhone(values.get(0));
                break;
            case "address":
                userEntity.getProfile().setAddress(values.get(0));
                break;
            case "avatar_url":
                userEntity.getProfile().setAvatarUrl(values.get(0));
            default:
                super.setAttribute(name, values);
        }
    }

    @Override
    public void removeAttribute(String name) {
        // Optional: implement custom attribute handling
        switch (name) {
            case "phone":
                userEntity.getProfile().setPhone(null);
                break;
            case "address":
                userEntity.getProfile().setAddress(null);
                break;
            case "avatar_url":
                userEntity.getProfile().setAvatarUrl(null);
        }
    }

    @Override
    public String getFirstAttribute(String name) {
        return switch (name) {
            case "phone" -> userEntity.getProfile().getPhone();
            case "address" -> userEntity.getProfile().getAddress();
            case "avatar_url" -> userEntity.getProfile().getAvatarUrl();
            default -> super.getFirstAttribute(name) ;
        };
    }

    @Override
    public Stream<String> getAttributeStream(String name) {
        return switch (name) {
            case "phone" -> Stream.of(userEntity.getProfile().getPhone());
            case "address" -> Stream.of(userEntity.getProfile().getAddress());
            case "avatar_url" -> Stream.of(userEntity.getProfile().getAvatarUrl());
            default -> super.getAttributeStream(name);
        };
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        Map<String, List<String>> attributes = new HashMap<>();
        UserProfile profile = userEntity.getProfile();
        if (profile != null) {
            attributes.put("phone", List.of(profile.getPhone()));
            attributes.put("address", List.of(profile.getAddress()));
            attributes.put("avatar_url", List.of(profile.getAvatarUrl()));
            attributes.put("first_name", List.of(profile.getFirstName()));
            attributes.put("last_name", List.of(profile.getLastName()));
        }
        return attributes;
    }

    @Override
    public String getFirstName() {
        return userEntity.getProfile() != null ? userEntity.getProfile().getFirstName() : "default";
    }

    @Override
    public void setFirstName(String firstName) {
        if (userEntity.getProfile() != null) {
            userEntity.getProfile().setFirstName(firstName);
        }
    }

    @Override
    public String getLastName() {
        return userEntity.getProfile() != null ? userEntity.getProfile().getLastName() : "default";
    }

    @Override
    public void setLastName(String lastName) {
        if (userEntity.getProfile() != null) {
            userEntity.getProfile().setLastName(lastName);
        }
    }

    @Override
    public String getEmail() {
        return userEntity.getEmail() != null ? userEntity.getEmail() : "default" + "@default.com";
    }

    @Override
    public void setEmail(String email) {
        userEntity.setEmail(email);
    }

    @Override
    public boolean isEmailVerified() {
        return userEntity.getIsVerifiedEmail() != null ? userEntity.getIsVerifiedEmail() : false;
    }

    @Override
    public void setEmailVerified(boolean verified) {
        userEntity.setIsVerifiedEmail(verified);
    }

    @Override
    public Stream<GroupModel> getGroupsStream() {
        return userEntity.getGroups() != null ? userEntity.getGroups().stream()
                .map(group -> new GroupAdapter(session, realm, group)) : Stream.empty();
    }

    @Override
    public Stream<RoleModel> getRoleMappingsStream() {
        Set<RoleModel> roles = new HashSet<>();

        if (userEntity.getRoles() != null) {
            roles.addAll(userEntity.getRoles().stream()
                    .map(role -> new RoleAdapter(session, realm, role))
                    .collect(Collectors.toSet()));
        }
        if (userEntity.getGroups() != null) {
            roles.addAll(userEntity.getGroups().stream()
                    .flatMap(group -> group.getRoles().stream())
                    .map(role -> new RoleAdapter(session, realm, role))
                    .collect(Collectors.toSet()));
        }
        return roles.stream();
    }

    public Set<Permission> getAllPermissions() {
        Set<Permission> permissions = new HashSet<>();

        if (userEntity.getRoles() == null) return permissions;

        for (Role role : userEntity.getRoles()) {
            permissions.addAll(role.getPermissions());
        }

        if (userEntity.getGroups() == null) return permissions;

        for (Group group : userEntity.getGroups()) {
            group.getRoles().forEach(role -> permissions.addAll(role.getPermissions()));
        }

        return permissions;
    }

//    public Stream<PermissionAdapter> getPermissionStream() {
//        return getAllPermissions().stream()
//                .map(PermissionAdapter::new);
//    }
}
