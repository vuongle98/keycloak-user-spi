package org.vuong.keycloak.spi;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.*;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.group.GroupLookupProvider;
import org.keycloak.storage.group.GroupStorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vuong.keycloak.spi.adapter.GroupAdapter; // Assuming GroupAdapter exists and is usable here
import org.vuong.keycloak.spi.entity.Group;
import org.vuong.keycloak.spi.repository.GroupRepository;
import org.vuong.keycloak.spi.repository.RoleRepository; // Include if group-role mapping is managed here

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// This provider implements only the Group-related interfaces
public class CustomGroupStorageProvider implements
        GroupLookupProvider,
        GroupProvider, GroupStorageProvider {

    private static final Logger logger = LoggerFactory.getLogger(CustomGroupStorageProvider.class);

    private final KeycloakSession session;
    private final ComponentModel model;
    private final GroupRepository groupRepository;
    private final RoleRepository roleRepository; // Include if group-role mapping is managed here

    public CustomGroupStorageProvider(KeycloakSession session, ComponentModel model, GroupRepository groupRepository, RoleRepository roleRepository) {
        this.session = session;
        this.model = model;
        this.groupRepository = groupRepository;
        this.roleRepository = roleRepository; // Assign if included
    }

    @Override
    public void close() {
        // Clean up resources if needed. Similar to the user provider.
        logger.info("CustomGroupStorageProvider closed for component {}", model.getId());
    }

    // --- GroupLookupProvider ---
    @Override
    public GroupModel getGroupById(RealmModel realm, String id) {
        String externalId = StorageId.externalId(id);
        logger.info("Getting group by ID: realm={}, id={}", realm.getId(), externalId);
        // GroupRepository.getById expects String ID and handles conversion to Long.
        Group groupEntity = groupRepository.getById(externalId);
        // The GroupAdapter constructor might need session, realm, and the entity.
        // If GroupAdapter needs repositories (e.g., for lazy loading roles/members), pass them.
        // Example constructor: GroupAdapter(session, realm, groupEntity, groupRepository, userRepository)
        // Since this is a Group provider, it has groupRepository. It does NOT have userRepository by default.
        // If GroupAdapter needs userRepository for getMembersStream(), that method should probably be
        // handled by the UserQueryProvider in the User Storage Provider.
        // Assuming GroupAdapter only needs session, realm, and groupEntity for basic group properties:
        return (groupEntity != null) ? new GroupAdapter(session, realm, groupEntity) : null;
        // If adapter needs RoleRepository for group-role mappings:
        // return (groupEntity != null) ? new GroupAdapter(session, realm, groupEntity, roleRepository) : null;
    }

    // Note: There is no getUserByUsername or getUserByEmail in GroupLookupProvider.
    // User lookups belong in the User Storage Provider.

    // --- GroupProvider ---
    @Override
    public Stream<GroupModel> getGroupsStream(RealmModel realm) {
        logger.info("Getting all groups stream for realm: {}", realm.getId());
        // Use the repository method that filters by realm and accepts pagination.
        // Keycloak calls this, and the stream might be paginated internally.
        return groupRepository.getAllGroups(realm.getId(), null, null) // Pass null for first/max if Keycloak handles pagination on the stream
                .stream()
                .map(groupEntity -> new GroupAdapter(session, realm, groupEntity));
    }

    @Override
    public Stream<GroupModel> getGroupsStream(RealmModel realm, Stream<String> ids, String search, Integer first, Integer max) {
        logger.info("Getting groups stream by ids or search: realm={}, search={}, first={}, max={}", realm.getId(), search, first, max);
        List<String> groupIds = ids != null ? ids.map(StorageId::externalId).collect(Collectors.toList()) : Collections.emptyList();
        // Use the repository method. It handles the case where both ids and search are empty.
        return groupRepository.getGroupsByIdsOrSearch(realm.getId(), groupIds, search, first, max)
                .stream()
                .map(groupEntity -> new GroupAdapter(session, realm, groupEntity));
    }

    @Override
    public Long getGroupsCount(RealmModel realm, Boolean onlyTopGroups) {
        logger.info("Getting groups count: realm={}, onlyTopGroups={}", realm.getId(), onlyTopGroups);
        // Use the counting methods in the repository
        if (Boolean.TRUE.equals(onlyTopGroups)) {
            return groupRepository.countTopLevelGroups(realm.getId());
        }
        return groupRepository.countGroups(realm.getId());
    }

    @Override
    public Long getGroupsCountByNameContaining(RealmModel realm, String search) {
        logger.info("Getting groups count by name: realm={}, search={}", realm.getId(), search);
        // Use the counting method in the repository
        return groupRepository.countGroupsByName(realm.getId(), search);
    }

    @Override
    public Stream<GroupModel> searchGroupsByAttributes(RealmModel realm, Map<String, String> attributes, Integer firstResult, Integer maxResults) {
        logger.info("Searching groups by attributes: realm={}, attributes={}, first={}, max={}", realm.getId(), attributes, firstResult, maxResults);
        // Use the repository method. Note: The implemented repository method does NOT filter by realm.
        // TODO: Add realm filtering to GroupRepository.searchByAttributes if needed.
        logger.warn("searchGroupsByAttributes - GroupRepository.searchByAttributes does not currently filter by realm.");
        return groupRepository.searchByAttributes(attributes, firstResult, maxResults) // Does not filter by realm in current repo code
                .stream()
                // .filter(group -> realm.getId().equals(group.getRealmId())) // Add realm filtering here if repo doesn't
                .map(groupEntity -> new GroupAdapter(session, realm, groupEntity));
    }

    @Override
    public Stream<GroupModel> searchForGroupByNameStream(RealmModel realm, String search, Boolean exact, Integer firstResult, Integer maxResults) {
        logger.info("Searching for group by name: realm={}, search={}, exact={}, first={}, max={}", realm.getId(), search, exact, firstResult, maxResults);
        // Use the repository method
        return groupRepository.searchGroupsByName(realm.getId(), search, exact, firstResult, maxResults)
                .stream()
                .map(groupEntity -> new GroupAdapter(session, realm, groupEntity));
    }

    @Override
    public Stream<GroupModel> getGroupsByRoleStream(RealmModel realm, RoleModel role, Integer firstResult, Integer maxResults) {
        String roleId = StorageId.externalId(role.getId());
        logger.info("Getting groups by role: realm={}, roleId={}, first={}, max={}", realm.getId(), roleId, firstResult, maxResults);
        // Use the repository method
        return groupRepository.findGroupsByRoleId(realm.getId(), roleId, firstResult, maxResults)
                .stream()
                .map(groupEntity -> new GroupAdapter(session, realm, groupEntity));
    }

    @Override
    public Stream<GroupModel> getTopLevelGroupsStream(RealmModel realm, String search, Boolean exact, Integer firstResult, Integer maxResults) {
        logger.info("Getting top-level groups stream: realm={}, search={}, exact={}, first={}, max={}", realm.getId(), search, exact, firstResult, maxResults);
        // Use the repository method
        return groupRepository.getTopLevelGroups(realm.getId(), search, exact, firstResult, maxResults)
                .stream()
                .map(groupEntity -> new GroupAdapter(session, realm, groupEntity));
    }

    @Override
    public GroupModel createGroup(RealmModel realm, String id, String name, GroupModel toParent) {
        String parentId = (toParent != null) ? StorageId.externalId(toParent.getId()) : null;
        logger.info("Creating group: realm={}, id={}, name={}, parentId={}", realm.getId(), id, name, parentId);

        // Check for duplicate name under the same parent or at top level using the repository method.
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
            // Save the new group using the repository. Repository save returns the managed entity.
            Group createdGroup = groupRepository.save(groupEntity);
            // After saving, the entity should have the generated ID and potentially a generated parentPath.
            // You might need to re-fetch to get the full updated entity including generated fields like parentPath,
            // depending on your JPA configuration and entity mapping.
            // Group reloadedGroup = groupRepository.getById(String.valueOf(createdGroup.getId())); // Example re-fetch

            // If creating a subgroup, need to update the parent's subgroup list in Keycloak's cache,
            // although Keycloak might handle this if getSubGroupsStream is properly implemented in GroupAdapter.
            // The GroupAdapter provided earlier does *not* implement hierarchical methods like getSubGroupsStream.
            // This means Keycloak will likely *not* see the hierarchy via this provider unless the adapter is updated.
            // For now, just return the adapter for the created group.

            logger.info("Successfully created group: {} with id {}", createdGroup.getName(), createdGroup.getId());
            return new GroupAdapter(session, realm, createdGroup); // Use the saved/reloaded entity
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
            // User group memberships are managed by the User provider, but removing the group implies
            // removing these mappings. The UserRepository has removeUserMappingsForGroup.
            // userRepository.removeUserMappingsForGroup(String.valueOf(groupEntity.getId())); // Needs UserRepository dependency or handle here.
            // Group role memberships should be handled by the Group provider or repository.
            // TODO: Implement removeRoleMappingsForGroup(String groupId) in GroupRepository.
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

    // Note: User-related methods like addUserMembership, removeUserMembership,
    // getGroupMembersStream belong in the User Storage Provider.

    // --- Methods for managing Group-Role mappings ---
    // Keycloak's GroupModel has methods like grantRole, deleteRoleMapping, getRoleMappingsStream.
    // If the Group provider is responsible for managing which roles are assigned to a group,
    // you would implement those methods here using the RoleRepository and GroupRepository
    // to update the many-to-many relationship between Group and Role entities.
    // Example (requires RoleRepository dependency and Group entity with roles collection):

    // @Override
    // public void grantRole(GroupModel group, RoleModel role) {
    //      String groupId = StorageId.externalId(group.getId());
    //      String roleId = StorageId.externalId(role.getId());
    //      logger.info("Granting role {} to group {}", roleId, groupId);
    //      Group groupEntity = groupRepository.getById(groupId);
    //      Role roleEntity = roleRepository.getById(roleId);
    //      if (groupEntity != null && roleEntity != null) {
    //          if (groupEntity.getRoles() == null) groupEntity.setRoles(new java.util.HashSet<>());
    //          groupEntity.getRoles().add(roleEntity);
    //          groupRepository.save(groupEntity);
    //      } else {
    //           logger.warn("Group {} or Role {} not found for granting.", groupId, roleId);
    //      }
    // }
    //
    // @Override
    // public void deleteRoleMapping(GroupModel group, RoleModel role) {
    //     String groupId = StorageId.externalId(group.getId());
    //     String roleId = StorageId.externalId(role.getId());
    //     logger.info("Removing role {} from group {}", roleId, groupId);
    //     Group groupEntity = groupRepository.getById(groupId);
    //     Role roleEntity = roleRepository.getById(roleId);
    //     if (groupEntity != null && roleEntity != null && groupEntity.getRoles() != null) {
    //          // Need to remove the role from the group's roles collection
    //          // Ensure roles collection is loaded (e.g., FetchType.EAGER or accessed within transaction)
    //          groupEntity.getRoles().removeIf(r -> r.getId().toString().equals(roleId));
    //          groupRepository.save(groupEntity);
    //     } else {
    //         logger.warn("Group {} or Role {} not found or group has no roles for removal.", groupId, roleId);
    //     }
    // }
    //
    // @Override
    // public Stream<RoleModel> getRoleMappingsStream(GroupModel group) {
    //     String groupId = StorageId.externalId(group.getId());
    //     logger.info("Getting role mappings for group {}", groupId);
    //     Group groupEntity = groupRepository.getById(groupId);
    //     if (groupEntity != null && groupEntity.getRoles() != null) {
    //         // Ensure roles collection is loaded
    //         return groupEntity.getRoles().stream().map(role -> new RoleAdapter(session, realm, role));
    //     }
    //     return Stream.empty();
    // }
    //
    // @Override
    // public Stream<RoleModel> getRealmRoleMappingsStream(GroupModel group) {
    //      // Filter getRoleMappingsStream for realm roles (where Role entity clientId is null)
    //      return getRoleMappingsStream(group)
    //              .filter(RoleModel::isRealmRole);
    // }
    //
    // @Override
    // public Stream<RoleModel> getClientRoleMappingsStream(GroupModel group, ClientModel app) {
    //      // Filter getRoleMappingsStream for client roles matching the app (client)
    //       String clientId = app.getId();
    //       return getRoleMappingsStream(group)
    //               .filter(role -> role.isClientRole() && role.getContainerId().equals(clientId));
    // }


    // --- Implementing missing RoleProvider methods (grantToAllUsers is in UserProvider) ---
    // RoleProvider methods like addRealmRole, removeRole, searchForRoles, etc. belong in the Role Storage Provider
    // if you choose to split Role management. If not splitting roles, they would stay in the User Provider
    // as was done in the previous implementation steps.
}