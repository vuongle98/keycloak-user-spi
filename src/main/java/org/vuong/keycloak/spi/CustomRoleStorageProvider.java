package org.vuong.keycloak.spi;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.*;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.role.RoleLookupProvider;
import org.keycloak.storage.role.RoleStorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vuong.keycloak.spi.adapter.RoleAdapter; // Assuming RoleAdapter exists
import org.vuong.keycloak.spi.entity.Role;
import org.vuong.keycloak.spi.repository.RoleRepository;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// This provider implements only the Role-related interfaces
public class CustomRoleStorageProvider implements
        RoleProvider, RoleStorageProvider {

    private static final Logger logger = LoggerFactory.getLogger(CustomRoleStorageProvider.class);

    private final KeycloakSession session;
    private final ComponentModel model;
    private final RoleRepository roleRepository;


    public CustomRoleStorageProvider(KeycloakSession session, ComponentModel model, RoleRepository roleRepository) {
        this.session = session;
        this.model = model;
        this.roleRepository = roleRepository;
    }

    @Override
    public void close() {
        // Clean up resources if needed.
        logger.info("CustomRoleStorageProvider closed for component {}", model.getId());
    }

    // --- RoleLookupProvider ---
    @Override
    public RoleModel getRealmRole(RealmModel realm, String name) {
        logger.info("Getting realm role: realm={}, name={}", realm.getId(), name);
        // Use the repository method
        Role roleEntity = roleRepository.findRealmRoleByName(realm.getId(), name);
        return (roleEntity != null) ? new RoleAdapter(session, realm, roleEntity) : null;
    }

    @Override
    public RoleModel getClientRole(ClientModel client, String name) {
        logger.info("Getting client role: client={}, name={}", client.getClientId(), name);
        // Use the repository method
        Role roleEntity = roleRepository.findClientRoleByName(client.getRealm().getId(), client.getId(), name);
        return (roleEntity != null) ? new RoleAdapter(session, client.getRealm(), roleEntity) : null;
    }

    // --- RoleProvider ---
    @Override
    public RoleModel getRoleById(RealmModel realm, String id) {
        String externalId = StorageId.externalId(id);
        logger.info("Getting role by ID: realm={}, id={}", realm.getId(), externalId);
        // Use the repository method
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
        // Use the repository method
        return roleRepository.searchRealmRoles(realm.getId(), search, first, max)
                .stream()
                .map(roleEntity -> new RoleAdapter(session, realm, roleEntity));
    }

    @Override
    public Stream<RoleModel> searchForClientRolesStream(ClientModel client, String search, Integer first, Integer max) {
        logger.info("Searching for client roles: client={}, search={}, first={}, max={}", client.getClientId(), search, first, max);
        // Use the repository method
        return roleRepository.searchClientRoles(client.getRealm().getId(), client.getId(), search, first, max)
                .stream()
                .map(roleEntity -> new RoleAdapter(session, client.getRealm(), roleEntity));
    }

    @Override
    public Stream<RoleModel> searchForClientRolesStream(RealmModel realm, Stream<String> clientIds, String search, Integer first, Integer max) {
        logger.info("Searching for client roles by client IDs: realm={}, search={}, first={}, max={}", realm.getId(), search, first, max);
        List<String> listOfClientIds = clientIds != null ? clientIds.collect(Collectors.toList()) : Collections.emptyList();
        // Use the repository method
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
        // Use the repository method
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
        // Check for duplicate name using the repository method.
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
        // Use the repository method
        return roleRepository.getRealmRoles(realm.getId(), first, max)
                .stream()
                .map(roleEntity -> new RoleAdapter(session, realm, roleEntity));
    }

    @Override
    public Stream<RoleModel> getRolesStream(RealmModel realm, Stream<String> ids, String search, Integer first, Integer max) {
        logger.info("Getting roles stream by ids or search: realm={}, search={}, first={}, max={}", realm.getId(), search, first, max);
        List<String> roleIds = ids != null ? ids.map(StorageId::externalId).collect(Collectors.toList()) : Collections.emptyList();
        // Use the repository method
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
        // Use the repository method
        Role roleEntity = roleRepository.getById(roleId);
        if (roleEntity == null) {
            logger.warn("Role not found for removal: {}", roleId);
            return false;
        }
        try {
            // Ensure to remove assignments from users and groups before deleting the role.
            // These are handled in the User and Group providers or their repositories.
            // Call methods on session to trigger cleanup in other providers.
            // Keycloak's preRemove(RoleModel) on RealmModel should ideally handle this cascading cleanup across providers.
            // If not automatically handled by Keycloak's SPI interaction or JPA cascades:
            // TODO: Ensure user assignments are removed (User provider/repo responsibility)
            // TODO: Ensure group assignments are removed (Group provider/repo responsibility)
            logger.warn("removeRole - User and Group assignment removal not explicitly called here. Relying on Keycloak cascade or repository implementation.");

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
            // These are handled in the User and Group providers or their repositories.
            // TODO: Ensure user realm role assignments are removed (User provider/repo responsibility)
            // TODO: Ensure group realm role assignments are removed (Group provider/repo responsibility)
            logger.warn("removeRoles(RealmModel) - User and Group assignment removal for realm roles not explicitly called here. Relying on Keycloak cascade or repository implementation.");

            // Then delete the realm roles using the repository method
            roleRepository.deleteAllRealmRoles(realm.getId()); // This method is now implemented
            logger.info("Successfully removed all realm roles for realm: {}", realm.getId());
        } catch (Exception e) {
            logger.error("Failed to remove all realm roles for realm {}: {}", realm.getId(), e.getMessage(), e);
            // Depending on requirements, you might re-throw or handle this failure
        }
    }

    @Override
    public RoleModel addClientRole(ClientModel client, String id, String name) {
        logger.info("Adding client role: client={}, id={}, name={}", client.getClientId(), id, name);
        // Check for duplicate name using the repository method.
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
        // Use the repository method
        return roleRepository.getClientRoles(client.getRealm().getId(), client.getId(), first, max)
                .stream()
                .map(roleEntity -> new RoleAdapter(session, client.getRealm(), roleEntity));
    }

    @Override
    public void removeRoles(ClientModel client) {
        logger.info("Removing all client roles for client: {}", client.getClientId());
        try {
            // Remove all assignments for client roles for this client first
            // These are handled in the User and Group providers or their repositories.
            // TODO: Ensure user client role assignments are removed (User provider/repo responsibility)
            // TODO: Ensure group client role assignments are removed (Group provider/repo responsibility)
            logger.warn("removeRoles(ClientModel) - User and Group assignment removal for client roles not explicitly called here. Relying on Keycloak cascade or repository implementation.");

            // Then delete the client roles using the repository method
            roleRepository.deleteAllClientRoles(client.getRealm().getId(), client.getId()); // This method is now implemented
            logger.info("Successfully removed all client roles for client: {}", client.getClientId());
        } catch (Exception e) {
            logger.error("Failed to remove all client roles for client {}: {}", client.getClientId(), e.getMessage(), e);
            // Depending on requirements, you might re-throw or handle this failure
        }
    }

    // Note: grantToAllUsers and removeRoleFromAllUsers are related to user-role assignment,
    // which are handled by the User provider in this split setup. They are implemented
    // in the modified CustomUserStorageProvider.java file.
    // Implementation examples:
    // @Override public void grantToAllUsers(RealmModel realm, RoleModel role) { ... } // Now in CustomUserStorageProvider
    // @Override public void removeRoleFromAllUsers(RealmModel realm, RoleModel role) { ... } // Belongs in CustomUserStorageProvider

}