package org.vuong.keycloak.spi;

import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.models.*;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.group.GroupLookupProvider;
import org.keycloak.storage.role.RoleLookupProvider;
import org.keycloak.storage.user.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vuong.keycloak.spi.adapter.GroupAdapter;
import org.vuong.keycloak.spi.adapter.RoleAdapter;
import org.vuong.keycloak.spi.adapter.UserAdapter;
import org.vuong.keycloak.spi.entity.Group;
import org.vuong.keycloak.spi.entity.Role;
import org.vuong.keycloak.spi.entity.UserEntity;
import org.vuong.keycloak.spi.repository.GroupRepository;
import org.vuong.keycloak.spi.repository.RoleRepository;
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
        RoleLookupProvider,
        RoleProvider,
        GroupLookupProvider,
        GroupProvider {

    private static final Logger logger = LoggerFactory.getLogger(CustomUserStorageProvider.class);

    private final KeycloakSession session;
    private final ComponentModel model;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final GroupRepository groupRepository;

    public CustomUserStorageProvider(KeycloakSession session, ComponentModel model, UserRepository userRepository, RoleRepository roleRepository, GroupRepository groupRepository) {
        this.session = session;
        this.model = model;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.groupRepository = groupRepository;
    }

    @Override
    public void close() {
        // Clean up resources if needed (e.g., close EntityManager/Session)
        // Note: If JpaProvider manages the EntityManager lifecycle per session/request,
        // this close might be less critical for the EM itself, but useful for the provider instance.
        logger.info("CustomUserStorageProvider closed for component {}", model.getId());
    }

    // User Lookup Provider implementation
    @Override
    public UserModel getUserById(RealmModel realm, String id) {
        String externalId = StorageId.externalId(id); // Use externalId if id is composite
        logger.info("Fetching user by ID: realm={}, id={}, externalId={}", realm.getId(), id, externalId);
        // UserRepository.getById expects String ID and handles conversion to Long
        UserEntity entity = userRepository.getById(externalId);
        if (entity == null) {
            logger.warn("User not found with ID: {}", externalId);
            return null;
        }
        // Pass repositories to UserAdapter if it needs to lazy load roles/groups or other attributes for the user
        return new UserAdapter(session, realm, model, entity);
    }

    @Override
    public UserModel getUserByUsername(RealmModel realm, String username) {
        logger.info("Fetching user by username: realm={}, username={}", realm.getId(), username);
        // UserRepository.findByUsername already implemented
        UserEntity entity = userRepository.findByUsername(username);
        if (entity == null) {
            logger.warn("User not found with username: {}", username);
            return null;
        }
        return new UserAdapter(session, realm, model, entity);
    }

    @Override
    public UserModel getUserByEmail(RealmModel realm, String email) {
        logger.info("Fetching user by email: realm={}, email={}", realm.getId(), email);
        // UserRepository.findByEmail already implemented
        UserEntity entity = userRepository.findByEmail(email);
        if (entity == null) {
            logger.warn("User not found with email: {}", email);
            return null;
        }
        return new UserAdapter(session, realm, model, entity);
    }

    // User Query Provider implementation
    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, Map<String, String> params, Integer firstResult, Integer maxResults) {
        logger.info("Searching for users: realm={}, params={}, firstResult={}, maxResults={}", realm.getId(), params, firstResult, maxResults);
        // Keycloak often uses specific constants for search keys like UserModel.USERNAME, UserModel.EMAIL, etc.
        // Adapt Keycloak's params map to fit your userRepository.search signature.
        String search = params.get(UserModel.SEARCH); // Keycloak's general search param
        String username = params.get(UserModel.USERNAME);
        String email = params.get(UserModel.EMAIL);
        // Add other parameters if needed based on your UserRepository capabilities
        // e.g., params.get(UserModel.FIRST_NAME), params.get(UserModel.LAST_NAME), params.get(UserModel.EMAIL_VERIFIED) etc.

        // Assuming userRepository.search method maps well enough, or adapting parameters:
        // The existing userRepository.search takes search, username, email, firstResult, maxResults.
        // It does NOT filter by realm in the provided UserRepository code. Add realm filtering there if necessary.
        return userRepository.search(search, username, email, firstResult, maxResults) // Adapt parameters as needed
                .stream()
                // .filter(user -> realm.getId().equals(user.getRealmId())) // Add realm filtering here if repo doesn't
                .map(entity -> new UserAdapter(session, realm, model, entity));
    }

    @Override
    public Stream<UserModel> getGroupMembersStream(RealmModel realm, GroupModel group, Integer firstResult, Integer maxResults) {
        String groupId = StorageId.externalId(group.getId());
        logger.info("Fetching members for group: realm={}, groupId={}, firstResult={}, maxResults={}", realm.getId(), groupId, firstResult, maxResults);
        // Assumes userRepository has a method to find users by group ID, with realm filtering and pagination.
        // This method is not present in the UserRepository code provided. You would need to add it.
        // Example signature: public List<UserEntity> findUsersByGroupId(String realmId, String groupId, Integer firstResult, Integer maxResults)
        // For now, return empty stream as the method isn't implemented in the repository.
        // TODO: Implement findUsersByGroupId in UserRepository
        logger.warn("getGroupMembersStream not fully implemented: UserRepository needs findUsersByGroupId method.");
        return Stream.empty(); // Placeholder
    }

    @Override
    public Stream<UserModel> searchForUserByUserAttributeStream(RealmModel realm, String attrName, String attrValue) {
        logger.info("Searching for user by attribute: realm={}, attrName={}, attrValue={}", realm.getId(), attrName, attrValue);
        // This is typically for custom attributes. Common attributes like username/email might be handled by searchForUserStream.
        // Your userRepository needs a method to find users by attribute name and value, with realm filtering.
        // This method is not present in the UserRepository code provided. You would need to add it.
        // Example signature: public List<UserEntity> findUsersByAttribute(String realmId, String attrName, String attrValue)
        // For now, return empty stream as the method isn't implemented in the repository.
        // TODO: Implement findUsersByAttribute in UserRepository
        logger.warn("searchForUserByUserAttributeStream not fully implemented: UserRepository needs findUsersByAttribute method.");
        return Stream.empty(); // Placeholder
    }

    @Override
    public int getUsersCount(RealmModel realm) {
        logger.info("Getting users count for realm: {}", realm.getId());
        // Update to use the repository method that counts users, filtered by realm.
        // Assuming userRepository now has countUsersByRealm.
        // TODO: Implement countUsersByRealm in UserRepository
        logger.warn("getUsersCount not fully implemented: UserRepository needs countUsersByRealm.");
        // return userRepository.countUsersByRealm(realm.getId()); // Needs implementation in UserRepository
        return userRepository.countUsers(); // Falling back to generic count if realm count isn't implemented
    }

    // User Registration Provider implementation
    @Override
    public UserModel addUser(RealmModel realm, String username) {
        logger.info("Adding new user: realm={}, username={}", realm.getId(), username);
        // Check for duplicate username (assuming username must be unique globally or per realm)
        // If uniqueness is per realm, update findByUsername or add findByUsernameAndRealm to UserRepository
        if (userRepository.findByUsername(username) != null) { // Or findByUsernameAndRealm(realm.getId(), username)
            logger.warn("User already exists with username: {}. Cannot add.", username);
            // Keycloak expects a null return or ModelDuplicateException in this case
            // throw new ModelDuplicateException("User with username " + username + " already exists.");
            return null; // Returning null indicates failure to add user
        }
        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setLocked(false); // Default to enabled
        // Set realm association if UserEntity has a realmId field
        // user.setRealmId(realm.getId()); // Assuming UserEntity has a realmId field

        // Need to create a UserProfile entity as well, since UserEntity has a non-null UserProfile relationship
        // This assumes UserProfile is mandatory and gets created with the user.
        // Adjust if UserProfile is optional or created separately.
        // TODO: Handle UserProfile creation for the new user (e.g., create new UserProfile and set it on user)
        // UserProfile profile = new UserProfile();
        // user.setProfile(profile); // Link the new profile to the user

        try {
            userRepository.save(user); // Save the new user (and profile via cascade if configured)
            // After saving, the entity should have the generated ID.
            // You might need to re-fetch or rely on the saved entity having the ID set by the persistence context.
            // UserEntity createdUser = userRepository.getById(String.valueOf(user.getId())); // Example re-fetch if needed
            logger.info("Successfully created user: {} with id {}", user.getUsername(), user.getId());

            return new UserAdapter(session, realm, model, user); // Use the saved entity
        } catch (Exception e) {
            logger.error("Failed to create user {}: {}", username, e.getMessage(), e);
            // Handle potential exceptions (e.g., database errors) and return null
            return null;
        }
    }

    @Override
    public boolean removeUser(RealmModel realm, UserModel user) {
        String externalId = StorageId.externalId(user.getId());
        logger.info("Removing user: realm={}, userId={}", realm.getId(), externalId);
        // UserRepository.delete(String userId) handles finding and deleting.
        // Ensure the repository's delete method correctly handles cascading deletes
        // for relationships like UserProfile, user-roles, user-groups if necessary.
        try {
            userRepository.delete(externalId);
            logger.info("Successfully deleted user: {}", externalId);
            return true;
        } catch (Exception e) {
            logger.error("Failed to delete user {}: {}", externalId, e.getMessage(), e);
            // Handle potential exceptions and return false
            return false;
        }
    }

    // Credential Input Validator implementation
    @Override
    public boolean supportsCredentialType(String credentialType) {
        return PasswordCredentialModel.TYPE.equals(credentialType);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        if (!supportsCredentialType(credentialType)) {
            return false;
        }
        // Fetch the user entity directly to check its password status
        String externalId = StorageId.externalId(user.getId());
        // UserRepository.getById expects String ID and handles conversion to Long
        UserEntity entity = userRepository.getById(externalId); // Use getById
        return entity != null && entity.getPassword() != null && !entity.getPassword().isEmpty();
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        if (!(input instanceof UserCredentialModel cred) || !supportsCredentialType(input.getType())) {
            logger.warn("Invalid credential type or input for user {}: type={}", user.getUsername(), input.getType());
            return false;
        }
        String externalId = StorageId.externalId(user.getId());
        // UserRepository.getById expects String ID and handles conversion to Long
        UserEntity entity = userRepository.getById(externalId); // Use getById
        if (entity == null || entity.getPassword() == null || entity.getPassword().isEmpty()) {
            logger.warn("User {} (ID: {}) not found or has no password set for validation.", user.getUsername(), externalId);
            return false;
        }
        // PasswordUtil handles hashing and comparison
        boolean valid = PasswordUtil.verifyPassword(cred.getChallengeResponse(), entity.getPassword());
        if (!valid) {
            logger.warn("Password validation failed for user {}", user.getUsername());
        } else {
            logger.info("Password validation successful for user {}", user.getUsername());
        }
        return valid;
    }

    // User Bulk Update Provider implementation
    @Override
    public void preRemove(RealmModel realm) {
        logger.info("Pre-remove realm: {}. All users, groups, and roles from this provider for this realm will be removed if not handled by Keycloak.", realm.getId());
        // If you need to perform specific cleanup in your external store when a realm is deleted
        // and your entities are linked to a realm ID:
        try {
            // Remove roles and groups first as they might have foreign keys referencing the realm
            // These methods now exist in the updated repositories and filter by realmId.
            roleRepository.deleteAllRealmRoles(realm.getId());
            // Assuming GroupRepository has deleteAllGroupsByRealm implemented
            // TODO: Implement deleteAllGroupsByRealm in GroupRepository
            logger.warn("preRemove(RealmModel) - deleteAllGroupsByRealm not fully implemented in GroupRepository.");
            // groupRepository.deleteAllGroupsByRealm(realm.getId()); // Needs implementation

            // Then remove users.
            // TODO: Implement deleteAllUsersByRealm in UserRepository
            logger.warn("preRemove(RealmModel) - deleteAllUsersByRealm not implemented in UserRepository.");
            // userRepository.deleteAllUsersByRealm(realm.getId()); // Needs implementation
        } catch (Exception e) {
            logger.error("Failed to perform pre-remove cleanup for realm {}: {}", realm.getId(), e.getMessage(), e);
            // Depending on requirements, you might re-throw or handle this failure
        }
    }

    @Override
    public void preRemove(RealmModel realm, GroupModel group) {
        String groupId = StorageId.externalId(group.getId());
        logger.info("Pre-remove group: {} ({}) from realm: {}", group.getName(), groupId, realm.getId());
        // If you need to clean up user-group memberships or other dependencies in your external store:
        // TODO: Implement removeUserMappingsForGroup(String groupId) in UserRepository
        logger.warn("preRemove(RealmModel, GroupModel) - removeUserMappingsForGroup not implemented in UserRepository.");
        // userRepository.removeUserMappingsForGroup(groupId); // Needs implementation
    }

    @Override
    public void preRemove(RealmModel realm, RoleModel role) {
        String roleId = StorageId.externalId(role.getId());
        logger.info("Pre-remove role: {} ({}) from realm: {}", role.getName(), roleId, realm.getId());
        // If you need to clean up user-role or group-role assignments in your external store:
        // TODO: Implement removeUserMappingsForRole(String roleId) in UserRepository
        // TODO: Implement removeGroupMappingsForRole(String roleId) in GroupRepository
        logger.warn("preRemove(RealmModel, RoleModel) - assignment removal not fully implemented in UserRepository/GroupRepository.");
        // userRepository.removeUserMappingsForRole(roleId); // Needs implementation
        // groupRepository.removeGroupMappingsForRole(roleId); // Needs implementation
    }


    // --- RoleLookupProvider ---
    @Override
    public RoleModel getRealmRole(RealmModel realm, String name) {
        logger.info("Getting realm role: realm={}, name={}", realm.getId(), name);
        // Use the newly implemented method in RoleRepository
        Role roleEntity = roleRepository.findRealmRoleByName(realm.getId(), name);
        return (roleEntity != null) ? new RoleAdapter(session, realm, roleEntity) : null;
    }

    @Override
    public RoleModel getClientRole(ClientModel client, String name) {
        logger.info("Getting client role: client={}, name={}", client.getClientId(), name);
        // Use the newly implemented method in RoleRepository
        Role roleEntity = roleRepository.findClientRoleByName(client.getRealm().getId(), client.getId(), name);
        return (roleEntity != null) ? new RoleAdapter(session, client.getRealm(), roleEntity) : null;
    }

    // --- RoleProvider ---
    @Override
    public RoleModel getRoleById(RealmModel realm, String id) {
        String externalId = StorageId.externalId(id);
        logger.info("Getting role by ID: realm={}, id={}", realm.getId(), externalId);
        // RoleRepository.getById expects String ID and handles conversion to Long
        Role roleEntity = roleRepository.getById(externalId);
        if (roleEntity == null) {
            return null;
        }
        // If your Role entity can be client-specific, you might need to get the client here
        // and potentially pass it to the RoleAdapter constructor if the adapter needs it.
        // ClientModel client = null;
        // if (roleEntity.getClientId() != null) { // Assuming RoleEntity has getClientId()
        //     client = realm.getClientById(roleEntity.getClientId());
        //     if (client == null) {
        //         logger.warn("Role {} has clientId {} but client not found in realm {}", externalId, roleEntity.getClientId(), realm.getId());
        //         // Decide how to handle roles linked to non-existent clients
        //     }
        // }
        return new RoleAdapter(session, realm, roleEntity); // Update adapter if client is needed
    }

    @Override
    public Stream<RoleModel> searchForRolesStream(RealmModel realm, String search, Integer first, Integer max) {
        logger.info("Searching for realm roles: realm={}, search={}, first={}, max={}", realm.getId(), search, first, max);
        // Use the newly implemented method in RoleRepository
        return roleRepository.searchRealmRoles(realm.getId(), search, first, max)
                .stream()
                .map(roleEntity -> new RoleAdapter(session, realm, roleEntity));
    }

    @Override
    public Stream<RoleModel> searchForClientRolesStream(ClientModel client, String search, Integer first, Integer max) {
        logger.info("Searching for client roles: client={}, search={}, first={}, max={}", client.getClientId(), search, first, max);
        // Use the newly implemented method in RoleRepository
        return roleRepository.searchClientRoles(client.getRealm().getId(), client.getId(), search, first, max)
                .stream()
                .map(roleEntity -> new RoleAdapter(session, client.getRealm(), roleEntity));
    }

    @Override
    public Stream<RoleModel> searchForClientRolesStream(RealmModel realm, Stream<String> clientIds, String search, Integer first, Integer max) {
        logger.info("Searching for client roles by client IDs: realm={}, search={}, first={}, max={}", realm.getId(), search, first, max);
        List<String> listOfClientIds = clientIds != null ? clientIds.collect(Collectors.toList()) : Collections.emptyList();
        // Use the newly implemented method in RoleRepository
        return roleRepository.searchClientRolesByClientIds(realm.getId(), listOfClientIds, search, first, max)
                .stream()
                .map(roleEntity -> {
                    // Need to get the client model if the RoleAdapter needs it.
                    // Assuming RoleEntity has getClientId()
                    // ClientModel client = realm.getClientById(roleEntity.getClientId());
                    return new RoleAdapter(session, realm, roleEntity); // Update adapter if client is needed
                });
    }

    @Override
    public Stream<RoleModel> searchForClientRolesStream(RealmModel realm, String search, Stream<String> excludedClientIds, Integer first, Integer max) {
        logger.info("Searching for client roles excluding client IDs: realm={}, search={}, first={}, max={}", realm.getId(), search, first, max);
        List<String> listOfExcludedClientIds = excludedClientIds != null ? excludedClientIds.collect(Collectors.toList()) : Collections.emptyList();
        // Use the newly implemented method in RoleRepository
        return roleRepository.searchClientRolesExcludingClientIds(realm.getId(), listOfExcludedClientIds, search, first, max)
                .stream()
                .map(roleEntity -> {
                    // Need to get the client model if the RoleAdapter needs it.
                    // Assuming RoleEntity has getClientId()
                    // ClientModel client = realm.getClientById(roleEntity.getClientId());
                    return new RoleAdapter(session, realm, roleEntity); // Update adapter if client is needed
                });
    }

    @Override
    public RoleModel addRealmRole(RealmModel realm, String id, String name) {
        logger.info("Adding realm role: realm={}, id={}, name={}", realm.getId(), id, name);
        // Check for duplicate name (assuming role names must be unique per realm for realm roles)
        // Use the newly implemented method for checking duplicates.
        if (roleRepository.findRealmRoleByName(realm.getId(), name) != null) {
            logger.warn("Realm role with name {} already exists in realm {}", name, realm.getId());
            // Keycloak expects null or ModelDuplicateException
            // throw new ModelDuplicateException("Role with name " + name + " already exists in this realm.");
            return null;
        }
        Role roleEntity = new Role();
        // Role entity ID is auto-generated (Long ID with GenerationType.IDENTITY), so ignore the provided 'id'.
        // roleEntity.setId((id != null && !id.isEmpty()) ? Long.valueOf(id) : null); // Remove or adjust
        roleEntity.setName(name);
        // Associate with realm and mark as realm role
        // Assuming RoleEntity has a realmId field
        // roleEntity.setRealmId(realm.getId());
        // Assuming RoleEntity has a clientId field and null means realm role
        // roleEntity.setClientId(null);

        try {
            roleRepository.save(roleEntity); // Save the new role
            // After saving, the entity should have the generated ID.
            // You might need to re-fetch or rely on the saved entity having the ID set by the persistence context.
            // Role createdRole = roleRepository.getById(String.valueOf(roleEntity.getId())); // Example re-fetch if needed
            logger.info("Successfully created realm role: {} with id {}", roleEntity.getName(), roleEntity.getId());
            return new RoleAdapter(session, realm, roleEntity); // Use the saved entity
        } catch (Exception e) {
            logger.error("Failed to add realm role {} in realm {}: {}", name, realm.getId(), e.getMessage(), e);
            return null;
        }
    }

    @Override
    public Stream<RoleModel> getRealmRolesStream(RealmModel realm, Integer first, Integer max) {
        logger.info("Getting realm roles stream: realm={}, first={}, max={}", realm.getId(), first, max);
        // Use the newly implemented method in RoleRepository
        return roleRepository.getRealmRoles(realm.getId(), first, max)
                .stream()
                .map(roleEntity -> new RoleAdapter(session, realm, roleEntity));
    }

    @Override
    public Stream<RoleModel> getRolesStream(RealmModel realm, Stream<String> ids, String search, Integer first, Integer max) {
        logger.info("Getting roles stream by ids or search: realm={}, search={}, first={}, max={}", realm.getId(), search, first, max);
        List<String> roleIds = ids != null ? ids.map(StorageId::externalId).collect(Collectors.toList()) : Collections.emptyList();
        // Use the newly implemented method in RoleRepository
        return roleRepository.getRolesByIdsOrSearch(realm.getId(), roleIds, search, first, max)
                .stream()
                .map(roleEntity -> {
//                    ClientModel client = null;
//                    if (roleEntity.getClientId() != null) { // Assuming RoleEntity has getClientId()
//                        client = realm.getClientById(roleEntity.getClientId());
//                    }
                    return new RoleAdapter(session, realm, roleEntity); // Update adapter if client is needed
                });
    }


    @Override
    public boolean removeRole(RoleModel roleModel) {
        if (roleModel == null) return false;
        String roleId = StorageId.externalId(roleModel.getId());
        logger.info("Removing role: roleId={}", roleId);
        // RoleRepository.getById expects String ID and handles conversion to Long
        Role roleEntity = roleRepository.getById(roleId);
        if (roleEntity == null) {
            logger.warn("Role not found for removal: {}", roleId);
            return false;
        }
        try {
            // Ensure to remove assignments from users and groups before deleting the role
            // if your database has foreign key constraints or if relationships aren't cascade-deleted.
            // TODO: Implement methods in UserRepository and GroupRepository to remove role assignments by roleId
            logger.warn("removeRole - assignment removal not fully implemented in UserRepository/GroupRepository.");
            // userRepository.removeAssignmentsForRole(roleId); // Example needs implementation
            // groupRepository.removeAssignmentsForRole(roleId); // Example needs implementation

            // Use the repository's delete method
            roleRepository.delete(roleEntity); // Assuming delete(Role) is implemented
            logger.info("Successfully removed role: {}", roleId);
            return true;
        } catch (Exception e) {
            logger.error("Failed to remove role {}: {}", roleId, e.getMessage(), e);
            // Handle potential exceptions and return false
            return false;
        }
    }

    @Override
    public void removeRoles(RealmModel realm) {
        logger.info("Removing all realm roles for realm: {}", realm.getId());
        try {
            // Remove all assignments for realm roles in this realm first
            // TODO: Implement deleteAllAssignmentsForRealmRoles in UserRepository/GroupRepository
            logger.warn("removeRoles(RealmModel) - assignment removal not fully implemented in UserRepository/GroupRepository.");
            // userRepository.deleteAllAssignmentsForRealmRoles(realm.getId()); // Needs implementation
            // groupRepository.deleteAllAssignmentsForRealmRoles(realm.getId()); // Needs implementation

            // Then delete the realm roles using the newly implemented method
            roleRepository.deleteAllRealmRoles(realm.getId());
            logger.info("Successfully removed all realm roles for realm: {}", realm.getId());
        } catch (Exception e) {
            logger.error("Failed to remove all realm roles for realm {}: {}", realm.getId(), e.getMessage(), e);
            // Depending on requirements, you might re-throw or handle this failure
        }
    }

    @Override
    public RoleModel addClientRole(ClientModel client, String id, String name) {
        logger.info("Adding client role: client={}, id={}, name={}", client.getClientId(), id, name);
        // Check for duplicate name (assuming client role names must be unique per client)
        // Use the newly implemented method for checking duplicates.
        if (roleRepository.findClientRoleByName(client.getRealm().getId(), client.getId(), name) != null) {
            logger.warn("Client role with name {} already exists for client {}", name, client.getClientId());
            // Keycloak expects null or ModelDuplicateException
            // throw new ModelDuplicateException("Role with name " + name + " already exists for this client.");
            return null;
        }
        Role roleEntity = new Role();
        // ID is auto-generated
        // roleEntity.setId((id != null && !id.isEmpty()) ? Long.valueOf(id) : null); // Remove or adjust
        roleEntity.setName(name);
        // Associate with client and realm
        // Assuming RoleEntity has clientId and realmId fields
        // roleEntity.setClientId(client.getId());
        // roleEntity.setRealmId(client.getRealm().getId());
        // roleEntity.setClientRole(true); // Or rely on clientId != null

        try {
            roleRepository.save(roleEntity); // Save the new role
            // Re-fetch or rely on saved entity having the ID
            // Role createdRole = roleRepository.getById(String.valueOf(roleEntity.getId())); // Example re-fetch if needed
            logger.info("Successfully created client role: {} for client {} with id {}", roleEntity.getName(), client.getClientId(), roleEntity.getId());
            return new RoleAdapter(session, client.getRealm(), roleEntity); // Use the saved entity
        } catch (Exception e) {
            logger.error("Failed to add client role {} for client {}: {}", name, client.getClientId(), e.getMessage(), e);
            return null;
        }
    }

    @Override
    public Stream<RoleModel> getClientRolesStream(ClientModel client, Integer first, Integer max) {
        logger.info("Getting client roles stream: client={}, first={}, max={}", client.getClientId(), first, max);
        // Use the newly implemented method in RoleRepository
        return roleRepository.getClientRoles(client.getRealm().getId(), client.getId(), first, max)
                .stream()
                .map(roleEntity -> new RoleAdapter(session, client.getRealm(), roleEntity));
    }

    @Override
    public void removeRoles(ClientModel client) {
        logger.info("Removing all client roles for client: {}", client.getClientId());
        try {
            // Remove all assignments for client roles for this client first
            // TODO: Implement deleteAllAssignmentsForClientRoles in UserRepository/GroupRepository
            logger.warn("removeRoles(ClientModel) - assignment removal not fully implemented in UserRepository/GroupRepository.");
            // userRepository.deleteAllAssignmentsForClientRoles(client.getId()); // Needs implementation
            // groupRepository.deleteAllAssignmentsForClientRoles(client.getId()); // Needs implementation

            // Then delete the client roles using the newly implemented method
            roleRepository.deleteAllClientRoles(client.getRealm().getId(), client.getId());
            logger.info("Successfully removed all client roles for client: {}", client.getClientId());
        } catch (Exception e) {
            logger.error("Failed to remove all client roles for client {}: {}", client.getClientId(), e.getMessage(), e);
            // Depending on requirements, you might re-throw or handle this failure
        }
    }

    // --- GroupLookupProvider ---
    @Override
    public GroupModel getGroupById(RealmModel realm, String id) {
        String externalId = StorageId.externalId(id);
        logger.info("Getting group by ID: realm={}, id={}", realm.getId(), externalId);
        // GroupRepository.getById expects String ID and handles conversion to Long.
        Group groupEntity = groupRepository.getById(externalId);
        // Pass groupRepository and userRepository for lazy loading members/roles if needed by GroupAdapter
        // The GroupAdapter constructor shown previously didn't take repositories. Update it if needed.
        // GroupAdapter(session, realm, groupEntity)
        return (groupEntity != null) ? new GroupAdapter(session, realm, groupEntity) : null; // Update if Adapter needs repos
    }

    // --- GroupProvider ---
    @Override
    public Stream<GroupModel> getGroupsStream(RealmModel realm) { // This is for all groups in the realm, often paginated by Keycloak later
        logger.info("Getting all groups stream for realm: {}", realm.getId());
        // Use the newly implemented getAllGroups method that filters by realm and accepts pagination.
        // Keycloak calls this method, and the result stream might be paginated internally by Keycloak.
        // Providing pagination here makes the backend more efficient for large datasets.
        return groupRepository.getAllGroups(realm.getId(), null, null) // Pass null for first/max if Keycloak handles pagination on the stream
                .stream()
                .map(groupEntity -> new GroupAdapter(session, realm, groupEntity));
    }

    // Note: Keycloak usually relies on the getGroupsStream with pagination and search/ids below.
    // The simple getGroupsStream(RealmModel realm) might be less critical for large deployments.

    @Override
    public Stream<GroupModel> getGroupsStream(RealmModel realm, Stream<String> ids, String search, Integer first, Integer max) {
        logger.info("Getting groups stream by ids or search: realm={}, search={}, first={}, max={}", realm.getId(), search, first, max);
        List<String> groupIds = ids != null ? ids.map(StorageId::externalId).collect(Collectors.toList()) : Collections.emptyList();
        // Use the newly implemented method in GroupRepository
        // The repository method handles the case where both ids and search are empty.
        return groupRepository.getGroupsByIdsOrSearch(realm.getId(), groupIds, search, first, max)
                .stream()
                .map(groupEntity -> new GroupAdapter(session, realm, groupEntity));
    }


    @Override
    public Long getGroupsCount(RealmModel realm, Boolean onlyTopGroups) {
        logger.info("Getting groups count: realm={}, onlyTopGroups={}", realm.getId(), onlyTopGroups);
        // Use the newly implemented counting methods in GroupRepository
        if (Boolean.TRUE.equals(onlyTopGroups)) {
            return groupRepository.countTopLevelGroups(realm.getId());
        }
        return groupRepository.countGroups(realm.getId());
    }

    @Override
    public Long getGroupsCountByNameContaining(RealmModel realm, String search) {
        logger.info("Getting groups count by name: realm={}, search={}", realm.getId(), search);
        // Use the newly implemented method in GroupRepository
        return groupRepository.countGroupsByName(realm.getId(), search);
    }

    @Override
    public Stream<GroupModel> searchGroupsByAttributes(RealmModel realm, Map<String, String> attributes, Integer firstResult, Integer maxResults) {
        logger.info("Searching groups by attributes: realm={}, attributes={}, first={}, max={}", realm.getId(), attributes, firstResult, maxResults);
        // Your GroupRepository.searchByAttributes needs to handle the map of attributes and pagination.
        // The implemented GroupRepository.searchByAttributes *does not* currently filter by realm.
        // TODO: Add realm filtering to GroupRepository.searchByAttributes
        logger.warn("searchGroupsByAttributes - GroupRepository.searchByAttributes does not currently filter by realm.");
        return groupRepository.searchByAttributes(attributes, firstResult, maxResults) // Does not filter by realm in current repo code
                .stream()
                // .filter(group -> realm.getId().equals(group.getRealmId())) // Add realm filtering here if repo doesn't
                .map(groupEntity -> new GroupAdapter(session, realm, groupEntity));
    }

    @Override
    public Stream<GroupModel> searchForGroupByNameStream(RealmModel realm, String search, Boolean exact, Integer firstResult, Integer maxResults) {
        logger.info("Searching for group by name: realm={}, search={}, exact={}, first={}, max={}", realm.getId(), search, exact, firstResult, maxResults);
        // Use the newly implemented method in GroupRepository
        return groupRepository.searchGroupsByName(realm.getId(), search, exact, firstResult, maxResults)
                .stream()
                .map(groupEntity -> new GroupAdapter(session, realm, groupEntity));
    }


    @Override
    public Stream<GroupModel> getGroupsByRoleStream(RealmModel realm, RoleModel role, Integer firstResult, Integer maxResults) {
        String roleId = StorageId.externalId(role.getId());
        logger.info("Getting groups by role: realm={}, roleId={}, first={}, max={}", realm.getId(), roleId, firstResult, maxResults);
        // Use the newly implemented method in GroupRepository
        return groupRepository.findGroupsByRoleId(realm.getId(), roleId, firstResult, maxResults)
                .stream()
                .map(groupEntity -> new GroupAdapter(session, realm, groupEntity));
    }

    @Override
    public Stream<GroupModel> getTopLevelGroupsStream(RealmModel realm, String search, Boolean exact, Integer firstResult, Integer maxResults) {
        logger.info("Getting top-level groups stream: realm={}, search={}, exact={}, first={}, max={}", realm.getId(), search, exact, firstResult, maxResults);
        // Use the newly implemented method in GroupRepository
        return groupRepository.getTopLevelGroups(realm.getId(), search, exact, firstResult, maxResults)
                .stream()
                .map(groupEntity -> new GroupAdapter(session, realm, groupEntity));
    }


    @Override
    public GroupModel createGroup(RealmModel realm, String id, String name, GroupModel toParent) {
        String parentId = (toParent != null) ? StorageId.externalId(toParent.getId()) : null;
        logger.info("Creating group: realm={}, id={}, name={}, parentId={}", realm.getId(), id, name, parentId);

        // Check for duplicate name under the same parent or at top level using the newly implemented method.
        if (groupRepository.findGroupByNameAndParent(realm.getId(), name, parentId) != null) {
            logger.warn("Group with name '{}' already exists under parent {} (or at top level) in realm {}", name, (toParent != null ? toParent.getName() : "top-level"), realm.getId());
            // Keycloak expects null or ModelDuplicateException
            // throw new ModelDuplicateException("Group with name " + name + " already exists here.");
            return null;
        }

        Group groupEntity = new Group();
        // Group entity ID is auto-generated (Long ID with GenerationType.IDENTITY), so ignore the provided 'id'.
        // groupEntity.setId((id != null && !id.isEmpty()) ? Long.valueOf(id) : null); // Remove or adjust
        groupEntity.setName(name);
        // Associate with realm
        // Assuming GroupEntity has a realmId field
        // groupEntity.setRealmId(realm.getId());
        if (parentId != null) {
            Long parentIdLong;
            try {
                parentIdLong = Long.valueOf(parentId);
            } catch (NumberFormatException e) {
                logger.warn("Invalid parent group ID format for creation: {}", parentId);
                return null; // Invalid parent ID format
            }
            groupEntity.setParentId(parentIdLong);
            // Keycloak also provides parentPath, which might be useful for the external system's hierarchy.
            // groupEntity.setParentPath(toParent.getPath()); // Needs parentPath field in GroupEntity and update logic
        }

        try {
            groupRepository.save(groupEntity); // Save the new group
            // After saving, the entity should have the generated ID and potentially a generated parentPath.
            // You might need to re-fetch to get the full updated entity including generated fields like parentPath.
            // Group createdGroup = groupRepository.getById(String.valueOf(groupEntity.getId())); // Example re-fetch

            // If creating a subgroup, need to update the parent's subgroup list in Keycloak's cache,
            // although Keycloak might handle this if getSubGroupsStream is properly implemented in GroupAdapter.
            // The GroupAdapter provided earlier does *not* implement hierarchical methods like getSubGroupsStream.
            // This means Keycloak will likely *not* see the hierarchy via this provider unless the adapter is updated.
            // For now, just return the adapter for the created group.

            logger.info("Successfully created group: {} with id {}", groupEntity.getName(), groupEntity.getId());
            return new GroupAdapter(session, realm, groupEntity); // Use the saved entity
        } catch (Exception e) {
            logger.error("Failed to create group {} in realm {}: {}", name, realm.getId(), e.getMessage(), e);
            return null;
        }
    }

    @Override
    public boolean removeGroup(RealmModel realm, GroupModel groupModel) {
        if (groupModel == null) return false;
        String groupId = StorageId.externalId(groupModel.getId());
        logger.info("Removing group: realm={}, groupId={}", realm.getId(), groupId);

        // The Group entity uses a Long ID, so convert the external ID string to Long.
        Long groupIdLong;
        try {
            groupIdLong = Long.valueOf(groupId);
        } catch (NumberFormatException e) {
            logger.warn("Invalid external group ID format for removal: {}", groupId);
            return false;
        }

        // Find the entity by its Long ID using the repository method that handles String ID.
        Group groupEntity = groupRepository.getById(String.valueOf(groupIdLong)); // Assuming getById takes String ID

        if (groupEntity == null) {
            logger.warn("Group not found for removal with ID: {}", groupId);
            return false;
        }

        try {
            // Ensure to remove associated users and roles from the group before deleting the group itself.
            // This might be handled by cascade deletes in JPA mapping, or requires explicit calls here.
            // The Group entity shows ManyToMany relationships with UserEntity and Role.
            // Your repository delete method or JPA cascade config should handle the join table entries.
            // If not handled automatically by JPA cascade/delete orphans, you would need calls like:
            logger.warn("removeGroup - assignment removal not fully implemented (users, roles).");
            // userRepository.removeUserMappingsForGroup(String.valueOf(groupEntity.getId())); // Needs implementation
            // groupRepository.removeRoleMappingsForGroup(String.valueOf(groupEntity.getId())); // Needs implementation

            // Use the repository's delete method. GroupRepository.delete(Group) is implemented.
            groupRepository.delete(groupEntity);
            logger.info("Successfully removed group: {}", groupId);
            return true;
        } catch (Exception e) {
            logger.error("Failed to remove group {}: {}", groupId, e.getMessage(), e);
            // Handle potential exceptions and return false
            return false;
        }
    }

    // --- Implementing missing GroupProvider methods ---

    @Override
    public void moveGroup(RealmModel realm, GroupModel groupModel, GroupModel toParent) {
        String groupId = StorageId.externalId(groupModel.getId());
        String parentId = (toParent != null) ? StorageId.externalId(toParent.getId()) : null;
        logger.info("Moving group: realm={}, groupId={}, toParentId={}", realm.getId(), groupId, parentId);

        // Find the external group entity
        Group groupEntity = groupRepository.getById(groupId);

        if (groupEntity == null) {
            logger.warn("Group not found for move operation with ID: {}", groupId);
            return; // Cannot move a non-existent group
        }

        // Determine the new parent entity and its ID
        Long newParentIdLong = null;
        if (parentId != null && !parentId.trim().isEmpty()) {
            try {
                newParentIdLong = Long.valueOf(parentId);
            } catch (NumberFormatException e) {
                logger.warn("Invalid target parent group ID format for move operation: {}", parentId, e);
                // Decide how to handle invalid parent ID format (log, throw exception, ignore)
                return;
            }
            // Optional: Verify the parent group exists
            // Group parentEntity = groupRepository.getById(parentId); // getById handles conversion
            // if (parentEntity == null) {
            //     logger.warn("Target parent group not found for move operation with ID: {}", parentId);
            //     return; // Cannot move to a non-existent parent
            // }
            // newParentIdLong = parentEntity.getId(); // Use the entity's actual ID
        }

        // Update the parentId field
        groupEntity.setParentId(newParentIdLong);

        // Update parentPath if your external system manages it this way.
        // This is often complex and depends on your hierarchy implementation.
        // Keycloak's default implementation manages paths. You might need to mirror that logic.
        // TODO: Implement parentPath update logic based on new parentId
        logger.warn("moveGroup - parentPath update logic not implemented.");
        if (newParentIdLong == null) {
            groupEntity.setParentPath(null); // Assuming top-level has null path
        } else {
            // Example (requires fetching parent and calculating path)
            // Group parentEntity = groupRepository.getById(parentId); // Already fetched or fetch here
            // if (parentEntity != null) {
            //    String parentPath = parentEntity.getParentPath(); // Get parent's path
            //    String newPath = (parentPath != null ? parentPath : "") + "/" + parentEntity.getId(); // Append parent ID
            //    groupEntity.setParentPath(newPath); // Set new path
            // }
        }


        try {
            groupRepository.save(groupEntity); // Save the updated group
            logger.info("Successfully moved group {} to parent {}", groupId, parentId);
        } catch (Exception e) {
            logger.error("Failed to move group {}: {}", groupId, e.getMessage(), e);
            // Depending on requirements, you might re-throw or handle this failure
        }
    }

    @Override
    public void addTopLevelGroup(RealmModel realm, GroupModel subGroupModel) {
        String groupId = StorageId.externalId(subGroupModel.getId());
        logger.info("Adding group to top level: realm={}, groupId={}", realm.getId(), groupId);

        // Find the external group entity
        Group groupEntity = groupRepository.getById(groupId);

        if (groupEntity == null) {
            logger.warn("Group not found for adding to top level with ID: {}", groupId);
            return; // Cannot operate on a non-existent group
        }

        // Set parentId to null to make it a top-level group
        groupEntity.setParentId(null);
        // Clear parentPath as it's now a top-level group
        groupEntity.setParentPath(null); // Assuming top-level has null path

        try {
            groupRepository.save(groupEntity); // Save the updated group
            logger.info("Successfully moved group {} to top level", groupId);
        } catch (Exception e) {
            logger.error("Failed to add group {} to top level: {}", groupId, e.getMessage(), e);
            // Depending on requirements, you might re-throw or handle this failure
        }
    }


    // --- Implementing missing RoleProvider methods ---

    @Override
    public void grantToAllUsers(RealmModel realm, RoleModel roleModel) {
        String roleId = StorageId.externalId(roleModel.getId());
        logger.info("Granting role {} to all users in realm {}", roleId, realm.getId());

        // Find the external role entity
        Role roleEntity = roleRepository.getById(roleId);

        if (roleEntity == null) {
            logger.warn("Role not found for granting to all users with ID: {}", roleId);
            return; // Cannot grant a non-existent role
        }

        // Retrieve all users in the realm
        // This requires a method in UserRepository to get all users, filtered by realm, potentially paginated.
        // TODO: Implement getAllUsers(String realmId, Integer first, Integer max) in UserRepository
        logger.warn("grantToAllUsers - UserRepository needs getAllUsers(String realmId, Integer first, Integer max).");

        // Example implementation assuming a getAllUsers(String realmId, Integer first, Integer max) method exists:
        // Note: Processing all users at once might be memory intensive for large numbers of users.
        // Consider processing in batches using pagination if necessary.
        int batchSize = 100; // Example batch size
        int firstResult = 0;
        List<UserEntity> users;
        do {
            users = userRepository.getAllUsers(realm.getId(), firstResult, batchSize); // Needs implementation
            if (users == null || users.isEmpty()) {
                break; // No more users
            }

            for (UserEntity user : users) {
                // Add the role to the user's roles set.
                // Ensure the user entity's roles collection is initialized (e.g., FetchType.LAZY and accessed within a transaction)
                // Or ensure the repository method fetches roles eagerly if needed here.
                logger.debug("Granting role {} to user {}", roleEntity.getName(), user.getUsername());
                if (user.getRoles() == null) {
                    user.setRoles(new HashSet<>()); // Initialize if null
                }
                user.getRoles().add(roleEntity);
                // Save the updated user entity.
                // This can be inefficient saving one by one.
                logger.debug("Saving user {} after granting role", user.getUsername());
                userRepository.save(user); // Assuming userRepository.save handles update
            }

            firstResult += batchSize;
        } while (!users.isEmpty()); // Continue if we got a full batch

        logger.info("Finished attempting to grant role {} to all users in realm {}", roleId, realm.getId());
    }
}