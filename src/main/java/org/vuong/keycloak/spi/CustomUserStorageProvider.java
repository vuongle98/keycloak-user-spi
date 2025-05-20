package org.vuong.keycloak.spi;

import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.models.*;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.federated.UserFederatedStorageProvider;
import org.keycloak.storage.federated.UserGroupMembershipFederatedStorage;
import org.keycloak.storage.role.RoleStorageProvider;
import org.keycloak.storage.role.RoleStorageProviderFactory;
import org.keycloak.storage.user.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vuong.keycloak.spi.adapter.RoleAdapter;
import org.vuong.keycloak.spi.adapter.UserAdapter;
import org.vuong.keycloak.spi.entity.Group;
import org.vuong.keycloak.spi.entity.Role;
import org.vuong.keycloak.spi.entity.UserEntity;
import org.vuong.keycloak.spi.repository.GroupRepository;
import org.vuong.keycloak.spi.repository.RoleRepository;
import org.vuong.keycloak.spi.repository.UserProfileRepository;
import org.vuong.keycloak.spi.repository.UserRepository;
import org.vuong.keycloak.spi.util.PasswordUtil;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CustomUserStorageProvider implements
        UserStorageProvider,
        UserLookupProvider,
        UserQueryProvider,
        UserRegistrationProvider,
        CredentialInputValidator,
        UserBulkUpdateProvider,
        RoleStorageProvider,
        RoleStorageProviderFactory
{

    private static final Logger logger = LoggerFactory.getLogger(CustomUserStorageProvider.class);

    private final KeycloakSession session;
    private final ComponentModel model;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository; // Needed for user-centric and provider role methods
    private final GroupRepository groupRepository; // Needed for user-centric and provider group methods
    private final UserProfileRepository profileRepository; // Needed for user-centric and provider group methods


    // Constructor now accepts all repositories
    public CustomUserStorageProvider(KeycloakSession session, ComponentModel model, UserRepository userRepository, RoleRepository roleRepository, GroupRepository groupRepository, UserProfileRepository profileRepository) {
        this.session = session;
        this.model = model;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.groupRepository = groupRepository;
        this.profileRepository = profileRepository;
    }

    @Override
    public RoleStorageProvider create(KeycloakSession session, ComponentModel model) {
        return null;
    }

    @Override
    public String getId() {
        return "";
    }

    @Override
    public void close() {
        logger.info("CustomUserStorageProvider closed for component {}", model.getId());
    }

    // --- User Lookup Provider implementation ---
    // (Keep methods from the previous CustomUserStorageProvider)

    @Override
    public UserModel getUserById(RealmModel realm, String id) {
        logger.info("getUserById received raw id: {}", id);
        String externalId = StorageId.externalId(id);
        UserEntity entity = null;
        if (externalId != null) {
            logger.info("Attempting lookup by external ID: {}", externalId);
            entity = userRepository.getById(externalId);
        } else {
            logger.info("External ID extraction returned null for id: {}", id);
            logger.info("Attempting lookup by Keycloak ID: {}", id);
            entity = userRepository.findByKeycloakId(id);
        }
        if (entity == null) {
            logger.warn("User not found with ID (external or keycloak): {}", id);
            return null;
        }
        logger.info("Found user entity with ID: {}", entity.getId());
        // Pass all necessary repositories to the UserAdapter constructor
        return new UserAdapter(session, realm, model, entity, groupRepository, roleRepository, userRepository);
    }

    @Override
    public UserModel getUserByUsername(RealmModel realm, String username) {
        logger.info("Fetching user by username: realm={}, username={}", realm.getId(), username);
        UserEntity entity = userRepository.findByUsername(username);
        if (entity == null) {
            logger.warn("User not found with username: {}", username);
            return null;
        }
        // Pass all necessary repositories to the UserAdapter constructor
        return new UserAdapter(session, realm, model, entity, groupRepository, roleRepository, userRepository);
    }

    @Override
    public UserModel getUserByEmail(RealmModel realm, String email) {
        logger.info("Fetching user by email: realm={}, email={}", realm.getId(), email);
        UserEntity entity = userRepository.findByEmail(email);
        if (entity == null) {
            logger.warn("User not found with email: {}", email);
            return null;
        }
        // Pass all necessary repositories to the UserAdapter constructor
        return new UserAdapter(session, realm, model, entity, groupRepository, roleRepository, userRepository);
    }

    // --- User Query Provider implementation ---
    // (Keep methods from the previous CustomUserStorageProvider)

    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, Map<String, String> params, Integer firstResult, Integer maxResults) {
        logger.info("Searching for users: realm={}, params={}, firstResult={}, maxResults={}", realm.getId(), params, firstResult, maxResults);
        String search = params.get(UserModel.SEARCH);
        String username = params.get(UserModel.USERNAME);
        String email = params.get(UserModel.EMAIL);

        return userRepository.search(search, username, email, firstResult, maxResults)
                .stream()
                // Pass all necessary repositories to the UserAdapter constructor
                .map(entity -> new UserAdapter(session, realm, model, entity, groupRepository, roleRepository, userRepository));
    }

    @Override
    public Stream<UserModel> getGroupMembersStream(RealmModel realm, GroupModel group, Integer firstResult, Integer maxResults) {
        String keycloakGroupId = group.getId(); // This is Keycloak's ID for the group
        logger.info("getGroupMembersStream: Fetching members for group: realm={}, keycloakGroupId={}", realm.getId(), keycloakGroupId);

        // This call is crucial: it should resolve keycloakGroupId to your internal Group entity
        Group groupEntity = groupRepository.getById(keycloakGroupId); // getById handles both external Long and Keycloak UUID

        if (groupEntity == null) {
            logger.warn("getGroupMembersStream: Group entity not found for Keycloak ID: {}. Cannot fetch members.", keycloakGroupId);
            return Stream.empty(); // Return empty stream if group not found
        }

        // Assuming groupEntity.getId() is the external Long ID you use in your DB for user-group mapping
        String externalGroupId = String.valueOf(groupEntity.getId());
        logger.info("getGroupMembersStream: Resolved group Keycloak ID {} to external ID {}. Fetching users for this group.", keycloakGroupId, externalGroupId);

        List<UserEntity> usersInGroup = userRepository.findUsersByGroupId(realm.getId(), externalGroupId, firstResult, maxResults);
        logger.info("getGroupMembersStream: Found {} users for group {} (external ID {}).", usersInGroup.size(), group.getName(), externalGroupId);

        // Call UserRepository with the external ID of your group
        return usersInGroup
                .stream()
                // Pass all necessary repositories to the UserAdapter constructor
                .map(entity -> new UserAdapter(session, realm, model, entity, groupRepository, roleRepository, userRepository)); // <--- IMPORTANT: ensure userRepository is passed here
    }

    @Override
    public Stream<UserModel> searchForUserByUserAttributeStream(RealmModel realm, String attrName, String attrValue) {
        logger.info("Searching for user by attribute: realm={}, attrName={}, attrValue={}", realm.getId(), attrName, attrValue);
        return userRepository.findUsersByAttribute(realm.getId(), attrName, attrValue)
                .stream()
                // Pass all necessary repositories to the UserAdapter constructor
                .map(entity -> new UserAdapter(session, realm, model, entity, groupRepository, roleRepository, userRepository));
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
        user.setRealmId(realm.getId()); // FIX: Ensure realmId is set for new users
        // TODO: Handle UserProfile creation

        try {
            UserEntity createdUser = userRepository.save(user);
            // Pass all necessary repositories to the UserAdapter constructor
            UserModel newUserModel = new UserAdapter(session, realm, model, createdUser, groupRepository, roleRepository, userRepository);
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
    // (Keep methods from the previous CustomUserStorageProvider)

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
    // (Keep methods from the previous CustomUserStorageProvider)

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
            throw e;
        }
    }

    @Override
    public void grantToAllUsers(RealmModel realm, RoleModel role) {

    }

    // --- RoleStorageProvider implementation ---


    @Override
    public RoleModel getRealmRole(RealmModel realm, String name) {
        logger.info("getRealmRole: Looking up realm role by name '{}' in realm '{}' (specific method)", name, realm.getId());
        // This method is often a duplicate of getRoleByName for realm roles.
        // Returning null as per your instruction.
        return null;
    }

    @Override
    public RoleModel getRoleById(RealmModel realm, String id) {
        logger.info("getRoleById: Looking up role by ID '{}' in realm '{}'", id, realm.getId());
        Role roleEntity = roleRepository.getById(id);
        if (roleEntity != null) {
            if (Objects.equals(roleEntity.getRealmId(), realm.getId()) && roleEntity.getClientId() == null) {
                logger.info("getRoleById: Found realm role '{}' with ID '{}'", roleEntity.getName(), id);
                return new RoleAdapter(session, realm, model, roleEntity, roleRepository);
            } else {
                logger.warn("getRoleById: Role found with ID '{}' but does not belong to realm '{}' or is a client role.", id, realm.getId());
            }
        } else {
            logger.debug("getRoleById: No role found with ID '{}'", id);
        }
        return null;
    }

    // New method as per your request
    @Override
    public Stream<RoleModel> searchForRolesStream(RealmModel realm, String search, Integer first, Integer max) {
        logger.info("searchForRolesStream: Searching for all roles in realm '{}' with search '{}', first={}, max={}", realm.getId(), search, first, max);
        // This method should search both realm and client roles.
        // For simplicity, returning empty as per your instruction.
        return Stream.empty();
    }

    @Override
    public RoleModel getClientRole(ClientModel client, String name) {
        logger.info("getClientRole: Looking up client role by name '{}' for client '{}' in realm '{}' (specific method)", name, client.getClientId(), client.getRealm().getId());
        // This method is often a duplicate of getClientRoleByName.
        // Returning null as per your instruction.
        return null;
    }


    @Override
    public Stream<RoleModel> searchForClientRolesStream(ClientModel client, String search, Integer first, Integer max) {
        logger.info("searchForClientRolesStream: Searching for client roles for client '{}' with search '{}', first={}, max={}", client.getClientId(), search, first, max);
        return roleRepository.searchClientRoles(client.getRealm().getId(), client.getClientId(), search, first, max)
                .stream()
                .map(roleEntity -> new RoleAdapter(session, client.getRealm(), model, roleEntity, roleRepository));
    }

    // New method as per your request
    @Override
    public Stream<RoleModel> searchForClientRolesStream(RealmModel realm, Stream<String> ids, String search, Integer first, Integer max) {
        List<String> roleIds = ids != null ? ids.collect(Collectors.toList()) : Collections.emptyList();
        logger.info("searchForClientRolesStream (by Realm and IDs): Searching for client roles in realm '{}' with IDs {} and search '{}', first={}, max={}", realm.getId(), roleIds, search, first, max);
        // Returning empty as per your instruction.
        return Stream.empty();
    }

    // New method as per your request
    @Override
    public Stream<RoleModel> searchForClientRolesStream(RealmModel realm, String search, Stream<String> excludedIds, Integer first, Integer max) {
        List<String> excludedRoleIds = excludedIds != null ? excludedIds.collect(Collectors.toList()) : Collections.emptyList();
        logger.info("searchForClientRolesStream (by Realm and Excluded IDs): Searching for client roles in realm '{}' with search '{}' excluding IDs {}, first={}, max={}", realm.getId(), search, excludedRoleIds, first, max);
        // Returning empty as per your instruction.
        return Stream.empty();
    }
}
