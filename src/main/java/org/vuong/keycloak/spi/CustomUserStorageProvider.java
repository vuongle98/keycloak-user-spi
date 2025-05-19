package org.vuong.keycloak.spi;

import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.models.*;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vuong.keycloak.spi.adapter.UserAdapter; // Import UserAdapter
import org.vuong.keycloak.spi.entity.Group; // Keep if needed for preRemove or user-centric assignments
import org.vuong.keycloak.spi.entity.Role; // Keep if needed for preRemove or user-centric assignments
import org.vuong.keycloak.spi.entity.UserEntity; // Keep if needed for provider methods
import org.vuong.keycloak.spi.repository.GroupRepository; // Keep if needed for preRemove or user-centric assignments
import org.vuong.keycloak.spi.repository.RoleRepository; // Keep if needed for preRemove or user-centric assignments
import org.vuong.keycloak.spi.repository.UserRepository; // Keep if needed for provider methods
import org.vuong.keycloak.spi.util.PasswordUtil; // Keep if needed for credential validation

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CustomUserStorageProvider implements
        UserStorageProvider,
        UserLookupProvider,
        UserQueryProvider,
        UserRegistrationProvider,
        CredentialInputValidator,
        UserBulkUpdateProvider
{

    private static final Logger logger = LoggerFactory.getLogger(CustomUserStorageProvider.class);

    private final KeycloakSession session;
    private final ComponentModel model;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository; // Needed to pass to UserAdapter
    private final GroupRepository groupRepository; // Needed to pass to UserAdapter


    public CustomUserStorageProvider(KeycloakSession session, ComponentModel model, UserRepository userRepository, RoleRepository roleRepository, GroupRepository groupRepository) {
        this.session = session;
        this.model = model;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.groupRepository = groupRepository;
    }

    @Override
    public void close() {
        logger.info("CustomUserStorageProvider closed for component {}", model.getId());
    }

    // --- User Lookup Provider implementation ---
    @Override
    public UserModel getUserById(RealmModel realm, String id) {
        logger.info("getUserById received raw id: {}", id);

        String externalId = StorageId.externalId(id);
        UserEntity entity = null;

        if (externalId != null) {
            logger.info("Attempting lookup by external ID: {}", externalId);
            entity = userRepository.getById(externalId); // getById attempts Long conversion internally
        } else {
            logger.info("External ID extraction returned null for id: {}", id);
            logger.info("Attempting lookup by Keycloak ID: {}", id);
            entity = userRepository.findByKeycloakId(id); // New method to find by Keycloak UUID
        }


        if (entity == null) {
            logger.warn("User not found with ID (external or keycloak): {}", id);
            return null;
        }

        logger.info("Found user entity with ID: {}", entity.getId());
        // Create UserAdapter, passing groupRepository and roleRepository
        return new UserAdapter(session, realm, model, entity, groupRepository, roleRepository);
    }

    @Override
    public UserModel getUserByUsername(RealmModel realm, String username) {
        logger.info("Fetching user by username: realm={}, username={}", realm.getId(), username);
        UserEntity entity = userRepository.findByUsername(username);
        if (entity == null) {
            logger.warn("User not found with username: {}", username);
            return null;
        }
        // Create UserAdapter, passing groupRepository and roleRepository
        return new UserAdapter(session, realm, model, entity, groupRepository, roleRepository);
    }

    @Override
    public UserModel getUserByEmail(RealmModel realm, String email) {
        logger.info("Fetching user by email: realm={}, email={}", realm.getId(), email);
        UserEntity entity = userRepository.findByEmail(email);
        if (entity == null) {
            logger.warn("User not found with email: {}", email);
            return null;
        }
        // Create UserAdapter, passing groupRepository and roleRepository
        return new UserAdapter(session, realm, model, entity, groupRepository, roleRepository);
    }

    // --- User Query Provider implementation ---
    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, Map<String, String> params, Integer firstResult, Integer maxResults) {
        logger.info("Searching for users: realm={}, params={}, firstResult={}, maxResults={}", realm.getId(), params, firstResult, maxResults);
        String search = params.get(UserModel.SEARCH);
        String username = params.get(UserModel.USERNAME);
        String email = params.get(UserModel.EMAIL);

        return userRepository.search(search, username, email, firstResult, maxResults)
                .stream()
                // Create UserAdapter, passing groupRepository and roleRepository
                .map(entity -> new UserAdapter(session, realm, model, entity, groupRepository, roleRepository));
    }

    @Override
    public Stream<UserModel> getGroupMembersStream(RealmModel realm, GroupModel group, Integer firstResult, Integer maxResults) {
        String keycloakGroupId = group.getId();
        logger.info("Fetching members for group: realm={}, keycloakGroupId={}", realm.getId(), keycloakGroupId);

        Group groupEntity = groupRepository.getById(keycloakGroupId);

        if (groupEntity == null) {
            logger.warn("Group entity not found for Keycloak ID: {}. Cannot fetch members.", keycloakGroupId);
            return Stream.empty();
        }

        String externalGroupId = String.valueOf(groupEntity.getId());

        return userRepository.findUsersByGroupId(realm.getId(), externalGroupId, firstResult, maxResults)
                .stream()
                // Create UserAdapter, passing groupRepository and roleRepository
                .map(entity -> new UserAdapter(session, realm, model, entity, groupRepository, roleRepository));
    }

    @Override
    public Stream<UserModel> searchForUserByUserAttributeStream(RealmModel realm, String attrName, String attrValue) {
        logger.info("Searching for user by attribute: realm={}, attrName={}, attrValue={}", realm.getId(), attrName, attrValue);
        return userRepository.findUsersByAttribute(realm.getId(), attrName, attrValue)
                .stream()
                // Create UserAdapter, passing groupRepository and roleRepository
                .map(entity -> new UserAdapter(session, realm, model, entity, groupRepository, roleRepository));
    }

    @Override
    public int getUsersCount(RealmModel realm) {
        logger.info("Getting users count for realm: {}", realm.getId());
        return userRepository.countUsersByRealm(realm.getId());
    }

    // --- User Registration Provider implementation ---
    @Override
    public UserModel addUser(RealmModel realm, String username) {
        logger.info("Adding new user: realm={}, username={}", realm.getId(), username);
        if (userRepository.findByUsername(username) != null) {
            logger.warn("User already exists with username: {}. Cannot add.", username);
            return null;
        }
        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setLocked(false);
        // user.setRealmId(realm.getId()); // Assuming UserEntity has a realmId field

        // TODO: Handle UserProfile creation for the new user if required upon user creation
        // user.setProfile(new UserProfile()); // Example if UserProfile is always present

        try {
            UserEntity createdUser = userRepository.save(user);

            // Create UserModel, passing groupRepository and roleRepository
            UserModel newUserModel = new UserAdapter(session, realm, model, createdUser, groupRepository, roleRepository);

            String keycloakUserId = newUserModel.getId();
            if (keycloakUserId != null) {
                UserEntity managedUser = userRepository.getById(String.valueOf(createdUser.getId()));
                if (managedUser != null) {
                    managedUser.setKeycloakId(keycloakUserId);
                    userRepository.save(managedUser);
                    logger.info("Saved Keycloak ID {} for user {}", keycloakUserId, createdUser.getUsername());
                } else {
                    logger.warn("Could not re-fetch user entity by ID {} to save Keycloak ID {}", createdUser.getId(), keycloakUserId);
                }
            } else {
                logger.warn("Keycloak did not provide a UUID for newly created user {}", createdUser.getUsername());
            }

            logger.info("Successfully created user: {} with id {}", user.getUsername(), user.getId());

            return newUserModel;
        } catch (Exception e) {
            logger.error("Failed to create user {}: {}", username, e.getMessage(), e);
            return null;
        }
    }


    @Override
    public boolean removeUser(RealmModel realm, UserModel user) {
        String userId = user.getId();
        logger.info("Removing user: realm={}, keycloakUserId={}", realm.getId(), userId);
        try {
            userRepository.delete(userId);
            logger.info("Successfully triggered deletion for user with Keycloak ID: {}", userId);
            return true;
        } catch (Exception e) {
            logger.error("Failed to delete user with Keycloak ID {}: {}", userId, e.getMessage(), e);
            return false;
        }
    }

    // --- Credential Input Validator implementation ---
    @Override
    public boolean supportsCredentialType(String credentialType) {
        return PasswordCredentialModel.TYPE.equals(credentialType);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        if (!supportsCredentialType(credentialType)) {
            return false;
        }
        String userId = user.getId();
        UserEntity entity = null;

        String externalId = StorageId.externalId(userId);
        if (externalId != null) {
            entity = userRepository.getById(externalId);
        }
        if (entity == null) {
            entity = userRepository.findByKeycloakId(userId);
        }

        return entity != null && entity.getPassword() != null && !entity.getPassword().isEmpty();
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        if (!(input instanceof UserCredentialModel cred) || !supportsCredentialType(input.getType())) {
            logger.warn("Invalid credential type or input for user {}: type={}", user.getUsername(), input.getType());
            return false;
        }
        String userId = user.getId();
        UserEntity entity = null;

        String externalId = StorageId.externalId(userId);
        if (externalId != null) {
            entity = userRepository.getById(externalId);
        }
        if (entity == null) {
            entity = userRepository.findByKeycloakId(userId);
        }


        if (entity == null || entity.getPassword() == null || entity.getPassword().isEmpty()) {
            logger.warn("User {} (ID: {}) not found or has no password set for validation.", user.getUsername(), userId);
            return false;
        }
        boolean valid = PasswordUtil.verifyPassword(cred.getChallengeResponse(), entity.getPassword());
        if (!valid) {
            logger.warn("Password validation failed for user {}", user.getUsername());
        } else {
            logger.info("Password validation successful for user {}", user.getUsername());
        }
        return valid;
    }

    // --- User Bulk Update Provider implementation ---
    @Override
    public void preRemove(RealmModel realm) {
        logger.info("Pre-remove realm: {}. Triggering cleanup in user storage.", realm.getId());
        try {
            userRepository.deleteAllUsersByRealm(realm.getId());
            logger.info("Successfully triggered user deletion for realm: {}", realm.getId());

        } catch (Exception e) {
            logger.error("Failed to perform pre-remove cleanup for realm {} in User provider: {}", realm.getId(), e.getMessage(), e);
        }
    }


    @Override
    public void preRemove(RealmModel realm, GroupModel group) {
        String keycloakGroupId = group.getId();
        logger.info("Pre-remove group: {} ({}) from realm: {}. Triggering cleanup in user storage.", group.getName(), keycloakGroupId, realm.getId());
        Group groupEntity = groupRepository.getById(keycloakGroupId);

        if (groupEntity != null) {
            String externalGroupId = String.valueOf(groupEntity.getId());
            try {
                userRepository.removeUserMappingsForGroup(externalGroupId);
                logger.info("Successfully removed user mappings for group {}", keycloakGroupId);
            } catch (Exception e) {
                logger.error("Failed to remove user mappings for group {}: {}", keycloakGroupId, e.getMessage(), e);
            }
        } else {
            logger.warn("Group entity not found for Keycloak ID: {}. Cannot remove user mappings.", keycloakGroupId);
        }
    }

    @Override
    public void preRemove(RealmModel realm, RoleModel role) {
        String roleId = StorageId.externalId(role.getId());
        logger.info("Pre-remove role: {} ({}) from realm: {}. Triggering cleanup in user storage.", role.getName(), roleId, realm.getId());
        try {
            userRepository.removeUserMappingsForRole(roleId);
            logger.info("Successfully removed user mappings for role {}", roleId);
        } catch (Exception e) {
            logger.error("Failed to remove user mappings for role {}: {}", roleId, e.getMessage(), e);
        }
    }

    // --- User-centric Assignment Methods (Stay here) ---
    // Methods like add/remove user-role, add/remove user-group memberships would be implemented here
    // using userRepository and potentially roleRepository/groupRepository to update the join tables.
    // Example implementations are commented out below.

    // @Override
    // public void grantRole(UserModel user, RoleModel role) { ... }
    //
    // @Override
    // public void deleteRoleMapping(UserModel user, RoleModel role) { ... }
    //
    // @Override
    // public void addToGroup(UserModel user, GroupModel group) { ... }
    //
    // @Override
    // public void removeFromGroup(UserModel user, GroupModel group) { ... }

    // --- RoleProvider method grantToAllUsers (Remains here) ---
    public void grantToAllUsers(RealmModel realm, RoleModel roleModel) {
        logger.info("Granting role {} to all users in realm {}", roleModel.getId(), realm.getId());

        String roleId = StorageId.externalId(roleModel.getId());
        Role roleEntity = roleRepository.getById(roleId);

        if (roleEntity == null) {
            logger.warn("Role not found for granting to all users with ID: {}", roleId);
            return;
        }

        int batchSize = 100;
        int firstResult = 0;
        List<UserEntity> users;
        do {
            users = userRepository.getAllUsers(realm.getId(), firstResult, batchSize);
            if (users == null || users.isEmpty()) {
                break;
            }

            for (UserEntity user : users) {
                logger.debug("Granting role {} to user {}", roleEntity.getName(), user.getUsername());
                if (user.getRoles() == null) {
                    user.setRoles(new HashSet<>());
                }
                user.getRoles().add(roleEntity);
                logger.debug("Saving user {} after granting role", user.getUsername());
                userRepository.save(user);
            }

            firstResult += batchSize;
        } while (users.size() == batchSize);

        logger.info("Finished attempting to grant role {} to all users in realm {}", roleId, realm.getId());
    }

    // TODO: Implement removeRoleFromAllUsers(RealmModel realm, RoleModel role)
}