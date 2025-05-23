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
import org.vuong.keycloak.spi.repository.UserRepository;

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
    private final UserRepository userRepository;
    private final GroupRepository groupRepository; // Add GroupRepository field
    private final RoleRepository roleRepository; // Add RoleRepository field
    private final ComponentModel storageProviderModel; // Already exists, used for GroupAdapter

    // Update constructor to accept repositories
    public UserAdapter(
            KeycloakSession session,
            RealmModel realm,
            ComponentModel model,
            UserEntity userEntity,
            GroupRepository groupRepository,
            RoleRepository roleRepository,
            UserRepository userRepository
    ) {
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
        this.userRepository = userRepository;
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
        if (userEntity.getGroups() == null) {
            return Stream.empty();
        }
        return userEntity.getGroups().stream()
                .map(groupEntity -> new GroupAdapter(session, realm, groupEntity, groupRepository, roleRepository, storageProviderModel));
    }

    @Override
    public void joinGroup(GroupModel groupModel) { // Renamed parameter to groupModel for clarity
        log.info("UserAdapter.joinGroup(): User '{}' (ID: {}) joining group '{}' (Keycloak ID: {})",
                userEntity.getUsername(), userEntity.getId(), groupModel.getName(), groupModel.getId());

        Group groupEntityToAdd = null;
        try {
            // Use the new syncGroup method to ensure the group exists and is up-to-date in your DB
            groupEntityToAdd = groupRepository.syncGroup(realm.getId(), groupModel);
        } catch (Exception e) {
            log.error("Failed to sync group '{}' (Keycloak ID: {}) during user join: {}",
                    groupModel.getName(), groupModel.getId(), e.getMessage(), e);
            // Decide how to handle this error. Throwing an exception might prevent the user from being added in Keycloak.
            // For now, we'll log and return, meaning the group won't be added to the user in your DB.
            return;
        }

        if (groupEntityToAdd == null) {
            log.warn("Failed to obtain or create group entity for group '{}' (Keycloak ID: {}). Cannot add user to group.",
                    groupModel.getName(), groupModel.getId());
            return;
        }

        // Check if the user is already a member of the group to avoid duplicates
        if (userEntity.getGroups() != null && userEntity.getGroups().contains(groupEntityToAdd)) {
            log.debug("User '{}' is already a member of group '{}'. No action needed.",
                    userEntity.getUsername(), groupModel.getName());
            return;
        }

        // Add the group entity to the user's collection
        if (userEntity.getGroups() == null) {
            userEntity.setGroups(new HashSet<>());
        }
        userEntity.getGroups().add(groupEntityToAdd);

        // Update the user in your database
        try {
            userRepository.save(userEntity);
            log.info("Successfully added user '{}' to group '{}' in database.", userEntity.getUsername(), groupEntityToAdd.getName());
        } catch (Exception e) {
            log.error("Failed to add user '{}' to group '{}' in database: {}", userEntity.getUsername(), groupEntityToAdd.getName(), e.getMessage(), e);
            throw e; // Re-throw to indicate failure to Keycloak
        }
    }

    @Override
    public void leaveGroup(GroupModel group) {
        log.info("UserAdapter.leaveGroup(): User '{}' (ID: {}) leaving group '{}' (ID: {})",
                userEntity.getUsername(), userEntity.getId(), group.getName(), group.getId());

        // Extract the external ID from the Keycloak GroupModel
        String externalGroupIdString = StorageId.externalId(group.getId());
        Long externalGroupId = null;
        if (externalGroupIdString != null) {
            try {
                externalGroupId = Long.valueOf(externalGroupIdString);
            } catch (NumberFormatException e) {
                log.warn("Failed to parse external group ID '{}' as Long for group '{}'.", externalGroupIdString, group.getName());
            }
        }

        // Find the group entity to remove. Priority: use external ID first, then Keycloak ID.
        Group groupEntityToRemove = null;
        if (externalGroupId != null) {
            groupEntityToRemove = groupRepository.getById(String.valueOf(externalGroupId));
        } else if (group.getId() != null) {
            groupEntityToRemove = groupRepository.findByKeycloakId(group.getId());
        }

        if (groupEntityToRemove == null) {
            log.warn("Attempted to remove user '{}' from non-existent or unmanageable group '{}' (Keycloak ID: {}).",
                    userEntity.getUsername(), group.getName(), group.getId());
            return;
        }

        // Remove the group entity from the user's collection
        if (userEntity.getGroups() != null) {
            boolean removed = userEntity.getGroups().remove(groupEntityToRemove);
            if (!removed) {
                log.debug("User '{}' was not a member of group '{}'. No action needed.",
                        userEntity.getUsername(), group.getName());
                return;
            }
        } else {
            log.debug("User '{}' has no groups. No action needed for group '{}'.",
                    userEntity.getUsername(), group.getName());
            return;
        }

        // Update the user in your database
        try {
            userRepository.save(userEntity);
            log.info("Successfully removed user '{}' from group '{}' in database.", userEntity.getUsername(), group.getName());
        } catch (Exception e) {
            log.error("Failed to remove user '{}' from group '{}' in database: {}", userEntity.getUsername(), group.getName(), e.getMessage(), e);
            // You might want to throw a RuntimeException or handle this more gracefully
        }
    }

    @Override
    public Stream<RoleModel> getRoleMappingsStream() {
        log.debug("UserAdapter.getRoleMappingsStream() for user {}", getUsername());
        Set<RoleModel> roles = new HashSet<>();

        // Add roles directly assigned to the user
        if (userEntity.getRoles() != null) {
            roles.addAll(userEntity.getRoles().stream()
                    .map(roleEntity -> {
                        String keycloakRoleId = roleEntity.getKeycloakId(); // Try to use the existing Keycloak ID

                        if (keycloakRoleId != null) {
                            return session.roles().getRoleById(realm, keycloakRoleId);
                        } else {
                            // This scenario ideally shouldn't happen for roles managed by your provider
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet()));
        }

        // Add roles inherited from groups
        if (userEntity.getGroups() != null) {
            roles.addAll(userEntity.getGroups().stream()
                    .flatMap(group -> {
                        return group.getRoles() != null ? group.getRoles().stream() : Stream.empty();
                    })
                    .map(roleEntity -> {
                        String keycloakRoleId = roleEntity.getKeycloakId(); // Try to use the existing Keycloak ID

                        if (keycloakRoleId != null) {
                            return session.roles().getRoleById(realm, keycloakRoleId);
                        } else {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet()));
        }

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

    @Override
    public boolean isMemberOf(GroupModel group) {
        log.debug("UserAdapter.isMemberOf() for user {} and group {}", getUsername(), group.getName());
        // Efficiently check if the user's group collection contains the group.
        // First, resolve the GroupModel to your Group entity.
        String externalGroupIdString = StorageId.externalId(group.getId());
        Long externalGroupId = null;
        if (externalGroupIdString != null) {
            try {
                externalGroupId = Long.valueOf(externalGroupIdString);
            } catch (NumberFormatException e) {
                // If it's not a Long, it might be a Keycloak UUID, try findByKeycloakId
            }
        }

        Group targetGroupEntity = null;
        if (externalGroupId != null) {
            targetGroupEntity = groupRepository.getById(String.valueOf(externalGroupId)); // Try by external Long ID
        } else if (group.getId() != null) {
            targetGroupEntity = groupRepository.findByKeycloakId(group.getId()); // Try by Keycloak UUID
        }

        if (targetGroupEntity == null) {
            log.warn("Target group '{}' (Keycloak ID: {}) not found in repository for isMemberOf check.", group.getName(), group.getId());
            return false;
        }

        return userEntity.getGroups() != null && userEntity.getGroups().contains(targetGroupEntity);
    }

    @Override
    public void grantRole(RoleModel roleModel) {
        log.info("UserAdapter.grantRole(): User '{}' (ID: {}) granting role '{}' (Keycloak ID: {})",
                userEntity.getUsername(), userEntity.getId(), roleModel.getName(), roleModel.getId());

        Role roleEntityToAdd = null;
        try {
            // Use the new syncRole method to ensure the role exists and is up-to-date in your DB
            roleEntityToAdd = roleRepository.syncRole(realm.getId(), roleModel);
        } catch (Exception e) {
            log.error("Failed to sync role '{}' (Keycloak ID: {}) during user role grant: {}",
                    roleModel.getName(), roleModel.getId(), e.getMessage(), e);
            return; // Fail gracefully or re-throw based on desired behavior
        }

        if (roleEntityToAdd == null) {
            log.warn("Failed to obtain or create role entity for role '{}' (Keycloak ID: {}). Cannot grant role to user.",
                    roleModel.getName(), roleModel.getId());
            return;
        }

        if (userEntity.getRoles() != null && userEntity.getRoles().contains(roleEntityToAdd)) {
            log.debug("User '{}' already has role '{}'. No action needed.",
                    userEntity.getUsername(), roleModel.getName());
            return;
        }

        if (userEntity.getRoles() == null) {
            userEntity.setRoles(new HashSet<>());
        }
        userEntity.getRoles().add(roleEntityToAdd);

        try {
            userRepository.save(userEntity);
            log.info("Successfully granted user '{}' role '{}' in database.", userEntity.getUsername(), roleEntityToAdd.getName());
        } catch (Exception e) {
            log.error("Failed to grant user '{}' role '{}' in database: {}", userEntity.getUsername(), roleEntityToAdd.getName(), e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public Stream<RoleModel> getRealmRoleMappingsStream() {
        log.debug("UserAdapter.getRealmRoleMappingsStream() for user {}", getUsername());
        if (userEntity.getRoles() == null) {
            return Stream.empty();
        }
        return userEntity.getRoles().stream()
                .filter(roleEntity -> roleEntity.getClientId() == null) // Filter for realm roles
                .map(roleEntity -> new RoleAdapter(session, realm, storageProviderModel, roleEntity, roleRepository));
    }

    @Override
    public Stream<RoleModel> getClientRoleMappingsStream(ClientModel client) {
        log.debug("UserAdapter.getClientRoleMappingsStream() for user {} and client {}", getUsername(), client.getClientId());
        if (userEntity.getRoles() == null) {
            return Stream.empty();
        }
        return userEntity.getRoles().stream()
                .filter(roleEntity -> client.getClientId().equals(roleEntity.getClientId())) // Filter for client roles
                .map(roleEntity -> new RoleAdapter(session, realm, storageProviderModel, roleEntity, roleRepository));
    }

    @Override
    public boolean hasRole(RoleModel roleModel) {
        log.debug("UserAdapter.hasRole() for user {} and role {}", getUsername(), roleModel.getName());
        Role targetRoleEntity = null;
        if (roleModel.getId() != null) {
            targetRoleEntity = roleRepository.findByKeycloakId(roleModel.getId());
        }
        if (targetRoleEntity == null) {
            String externalRoleIdString = StorageId.externalId(roleModel.getId());
            if (externalRoleIdString != null) {
                targetRoleEntity = roleRepository.getById(externalRoleIdString);
            }
        }

        if (targetRoleEntity == null) {
            log.warn("Target role '{}' (Keycloak ID: {}) not found in repository for hasRole check.", roleModel.getName(), roleModel.getId());
            return false;
        }
        return userEntity.getRoles() != null && userEntity.getRoles().contains(targetRoleEntity);
    }

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