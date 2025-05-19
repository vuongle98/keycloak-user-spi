package org.vuong.keycloak.spi.adapter;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.*;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.adapter.AbstractUserAdapterFederatedStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vuong.keycloak.spi.entity.*;
import org.vuong.keycloak.spi.repository.GroupRepository; // Import GroupRepository
import org.vuong.keycloak.spi.repository.RoleRepository; // Import RoleRepository

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
    private final GroupRepository groupRepository; // Add GroupRepository field
    private final RoleRepository roleRepository; // Add RoleRepository field
    private final ComponentModel storageProviderModel; // Already exists, used for GroupAdapter

    // Update constructor to accept repositories
    public UserAdapter(KeycloakSession session, RealmModel realm, ComponentModel model, UserEntity userEntity, GroupRepository groupRepository, RoleRepository roleRepository) {
        super(session, realm, model);
        this.session = session;
        this.realm = realm;
        this.userEntity = userEntity;
        // StorageId.keycloakId should use the provider alias from the model and the external user ID
        // Assuming userEntity.getId() is the external Long ID and model.getName() is the provider alias
        this.keycloakId = StorageId.keycloakId(model, String.valueOf(userEntity.getId())); // Correct Keycloak ID format
        this.groupRepository = groupRepository; // Assign GroupRepository
        this.roleRepository = roleRepository; // Assign RoleRepository
        this.storageProviderModel = model; // Assign model
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
        switch (name) {
            case "phone":
                if (userEntity.getProfile() != null) userEntity.getProfile().setPhone(value);
                break;
            case "address":
                if (userEntity.getProfile() != null) userEntity.getProfile().setAddress(value);
                break;
            case "avatar_url":
                if (userEntity.getProfile() != null) userEntity.getProfile().setAvatarUrl(value);
                break;
            case "first_name":
                if (userEntity.getProfile() != null) userEntity.getProfile().setFirstName(value);
                break;
            case "last_name":
                if (userEntity.getProfile() != null) userEntity.getProfile().setLastName(value);
                break;
            default:
                // For attributes not directly mapped to UserProfile fields, use AbstractUserAdapterFederatedStorage's handling
                super.setSingleAttribute(name, value); // Assuming base class handles a generic attribute table
        }
        // You might need to save the userEntity or profile here if changes aren't cascaded
        // session.getProvider(UserStorageProvider.class, storageProviderModel).onUpdateUser(realm, this); // Example using provider's update method
    }

    @Override
    public void setAttribute(String name, List<String> values) {
        switch (name) {
            case "phone":
                if (userEntity.getProfile() != null) userEntity.getProfile().setPhone(values != null && !values.isEmpty() ? values.get(0) : null);
                break;
            case "address":
                if (userEntity.getProfile() != null) userEntity.getProfile().setAddress(values != null && !values.isEmpty() ? values.get(0) : null);
                break;
            case "avatar_url":
                if (userEntity.getProfile() != null) userEntity.getProfile().setAvatarUrl(values != null && !values.isEmpty() ? values.get(0) : null);
                break;
            case "first_name":
                if (userEntity.getProfile() != null) userEntity.getProfile().setFirstName(values != null && !values.isEmpty() ? values.get(0) : null);
                break;
            case "last_name":
                if (userEntity.getProfile() != null) userEntity.getProfile().setLastName(values != null && !values.isEmpty() ? values.get(0) : null);
                break;
            default:
                // For attributes not directly mapped, use base class handling
                super.setAttribute(name, values); // Assuming base class handles a generic attribute table
        }
        // You might need to save the userEntity or profile here
        // session.getProvider(UserStorageProvider.class, storageProviderModel).onUpdateUser(realm, this);
    }

    @Override
    public void removeAttribute(String name) {
        switch (name) {
            case "phone":
                if (userEntity.getProfile() != null) userEntity.getProfile().setPhone(null);
                break;
            case "address":
                if (userEntity.getProfile() != null) userEntity.getProfile().setAddress(null);
                break;
            case "avatar_url":
                if (userEntity.getProfile() != null) userEntity.getProfile().setAvatarUrl(null);
                break;
            case "first_name":
                if (userEntity.getProfile() != null) userEntity.getProfile().setFirstName(null);
                break;
            case "last_name":
                if (userEntity.getProfile() != null) userEntity.getProfile().setLastName(null);
                break;
            default:
                // Use base class handling for other attributes
                super.removeAttribute(name); // Assuming base class handles a generic attribute table
        }
        // You might need to save the userEntity or profile here
        // session.getProvider(UserStorageProvider.class, storageProviderModel).onUpdateUser(realm, this);
    }

    @Override
    public String getFirstAttribute(String name) {
        return switch (name) {
            case "phone" -> userEntity.getProfile() != null ? userEntity.getProfile().getPhone() : null;
            case "address" -> userEntity.getProfile() != null ? userEntity.getProfile().getAddress() : null;
            case "avatar_url" -> userEntity.getProfile() != null ? userEntity.getProfile().getAvatarUrl() : null;
            case "first_name" -> userEntity.getProfile() != null ? userEntity.getProfile().getFirstName() : null;
            case "last_name" -> userEntity.getProfile() != null ? userEntity.getProfile().getLastName() : null;
            default -> super.getFirstAttribute(name); // Assuming base class handles a generic attribute table
        };
    }

    @Override
    public Stream<String> getAttributeStream(String name) {
        return switch (name) {
            case "phone" -> Stream.of(userEntity.getProfile() != null ? userEntity.getProfile().getPhone() : null).filter(Objects::nonNull);
            case "address" -> Stream.of(userEntity.getProfile() != null ? userEntity.getProfile().getAddress() : null).filter(Objects::nonNull);
            case "avatar_url" -> Stream.of(userEntity.getProfile() != null ? userEntity.getProfile().getAvatarUrl() : null).filter(Objects::nonNull);
            case "first_name" -> Stream.of(userEntity.getProfile() != null ? userEntity.getProfile().getFirstName() : null).filter(Objects::nonNull);
            case "last_name" -> Stream.of(userEntity.getProfile() != null ? userEntity.getProfile().getLastName() : null).filter(Objects::nonNull);
            default -> super.getAttributeStream(name); // Assuming base class handles a generic attribute table
        };
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        Map<String, List<String>> attributes = new HashMap<>();
        UserProfile profile = userEntity.getProfile();
        if (profile != null) {
            if (profile.getPhone() != null) attributes.put("phone", Collections.singletonList(profile.getPhone()));
            if (profile.getAddress() != null) attributes.put("address", Collections.singletonList(profile.getAddress()));
            if (profile.getAvatarUrl() != null) attributes.put("avatar_url", Collections.singletonList(profile.getAvatarUrl()));
            if (profile.getFirstName() != null) attributes.put("first_name", Collections.singletonList(profile.getFirstName()));
            if (profile.getLastName() != null) attributes.put("last_name", Collections.singletonList(profile.getLastName()));
        }
        // Get attributes from the base class's handling (if any)
        Map<String, List<String>> baseAttributes = super.getAttributes(); // Assuming base class handles a generic attribute table
        if (baseAttributes != null) {
            // Merge base attributes, prioritizing attributes from this adapter if they exist
            baseAttributes.forEach((key, value) -> attributes.computeIfAbsent(key, k -> value));
        }

        return attributes;
    }

    @Override
    public String getFirstName() {
        return userEntity.getProfile() != null ? userEntity.getProfile().getFirstName() : null; // Return null if not set
    }

    @Override
    public void setFirstName(String firstName) {
        if (userEntity.getProfile() != null) {
            userEntity.getProfile().setFirstName(firstName);
        }
    }

    @Override
    public String getLastName() {
        return userEntity.getProfile() != null ? userEntity.getProfile().getLastName() : null; // Return null if not set
    }

    @Override
    public void setLastName(String lastName) {
        if (userEntity.getProfile() != null) {
            userEntity.getProfile().setLastName(lastName);
        }
    }

    @Override
    public String getEmail() {
        return userEntity.getEmail(); // Return actual email or null
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
        log.debug("UserAdapter.getGroupsStream() for user {}", getUsername());
        // Correctly create GroupAdapter instances, passing all required dependencies
        return userEntity.getGroups() != null ? userEntity.getGroups().stream()
                .map(groupEntity -> new GroupAdapter(session, realm, groupEntity, groupRepository, roleRepository, storageProviderModel)) // Pass all dependencies
                : Stream.empty();
    }

    @Override
    public Stream<RoleModel> getRoleMappingsStream() {
        log.debug("UserAdapter.getRoleMappingsStream() for user {}", getUsername());
        Set<RoleModel> roles = new HashSet<>();

        // Add roles directly assigned to the user
        if (userEntity.getRoles() != null) {
            roles.addAll(userEntity.getRoles().stream()
                    .map(role -> new RoleAdapter(session, realm, role))
                    .collect(Collectors.toSet()));
        }

        // Add roles inherited from groups
        if (userEntity.getGroups() != null) {
            roles.addAll(userEntity.getGroups().stream()
                    .flatMap(group -> {
                        // Access roles from the GroupAdapter, which should fetch them from the DB
                        // This might require the Group entity's roles collection to be loaded or lazy-loaded within the adapter method
                        // Or use groupRepository to get roles for the group.
                        // Assuming Group.getRoles() is available or lazily loaded:
                        return group.getRoles() != null ? group.getRoles().stream() : Stream.empty();
                    })
                    .map(role -> new RoleAdapter(session, realm, role))
                    .collect(Collectors.toSet()));
        }
        log.debug("Found {} role mappings for user {}", roles.size(), getUsername());
        return roles.stream();
    }


    // The getAllPermissions and getPermissionStream methods are specific to your schema
    // and are not part of the standard UserModel interface. Keep them if you use them internally.
    public Set<Permission> getAllPermissions() {
        Set<Permission> permissions = new HashSet<>();

        if (userEntity.getRoles() != null) {
            for (Role role : userEntity.getRoles()) {
                if (role.getPermissions() != null) {
                    permissions.addAll(role.getPermissions());
                }
            }
        }

        if (userEntity.getGroups() != null) {
            for (Group group : userEntity.getGroups()) {
                if (group.getRoles() != null) {
                    for (Role role : group.getRoles()) {
                        if (role.getPermissions() != null) {
                            permissions.addAll(role.getPermissions());
                        }
                    }
                }
            }
        }

        return permissions;
    }

    //    public Stream<PermissionAdapter> getPermissionStream() {
    //        return getAllPermissions().stream()
    //                .map(PermissionAdapter::new);
    //    }

    // Optional: Override equals and hashCode if needed for consistent behavior
    // AbstractUserAdapterFederatedStorage provides basic implementations,
    // but you might want to use the external entity ID for robustness.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserModel)) return false; // Compare with UserModel interface
        UserModel that = (UserModel) o;
        // Compare by Keycloak ID
        return Objects.equals(this.getId(), that.getId());
        // Or compare by external entity ID if confident in adapter type
        // if (o instanceof UserAdapter) {
        //     return Objects.equals(this.userEntity.getId(), ((UserAdapter) o).userEntity.getId());
        // }
        // return false;
    }

    @Override
    public int hashCode() {
        // Use Keycloak ID for hashCode
        return Objects.hashCode(getId());
        // Or use external entity ID
        // return Objects.hashCode(userEntity.getId());
    }
}