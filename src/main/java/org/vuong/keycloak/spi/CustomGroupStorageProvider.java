package org.vuong.keycloak.spi;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.*;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.group.GroupLookupProvider; // Import GroupLookupProvider
import org.keycloak.storage.group.GroupStorageProvider; // Keep GroupStorageProvider interface
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vuong.keycloak.spi.adapter.GroupAdapter;
import org.vuong.keycloak.spi.entity.Group;
import org.vuong.keycloak.spi.repository.GroupRepository;
import org.vuong.keycloak.spi.repository.RoleRepository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// This provider implements the necessary Group storage interfaces
public class CustomGroupStorageProvider implements
        GroupLookupProvider, // Added GroupLookupProvider here
        GroupProvider,
        GroupStorageProvider // GroupStorageProvider is a marker interface
{

    private static final Logger logger = LoggerFactory.getLogger(CustomGroupStorageProvider.class);

    private final KeycloakSession session;
    private final ComponentModel model;
    private final GroupRepository groupRepository;
    private final RoleRepository roleRepository;

    public CustomGroupStorageProvider(KeycloakSession session, ComponentModel model, GroupRepository groupRepository, RoleRepository roleRepository) {
        this.session = session;
        this.model = model;
        this.groupRepository = groupRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    public void close() {
        logger.info("CustomGroupStorageProvider closed for component {}", model.getId());
    }

    // --- GroupLookupProvider ---
    @Override
    public GroupModel getGroupById(RealmModel realm, String id) {
        logger.info("getGroupById received raw id: {}", id);
        Group groupEntity = groupRepository.getById(id);
        if (groupEntity == null) {
            logger.warn("Group not found with ID (external or keycloak): {}", id);
            return null;
        }
        logger.info("Found group entity with ID: {}", groupEntity.getId());
        return new GroupAdapter(session, realm, groupEntity, groupRepository, roleRepository, this.model);
    }


    // --- GroupProvider ---
    @Override
    public Stream<GroupModel> getGroupsStream(RealmModel realm) {
        logger.info("Getting all groups stream for realm: {}", realm.getId());
        return groupRepository.getAllGroups(realm.getId(), null, null)
                .stream()
                .map(groupEntity -> new GroupAdapter(session, realm, groupEntity, groupRepository, roleRepository, this.model));
    }

    @Override
    public Stream<GroupModel> getGroupsStream(RealmModel realm, Stream<String> ids, String search, Integer first, Integer max) {
        logger.info("Getting groups stream by ids or search: realm={}, search={}, first={}, max={}", realm.getId(), search, first, max);
        List<String> rawOrExtractedIds = ids != null ? ids.collect(Collectors.toList()) : Collections.emptyList();
        return groupRepository.getGroupsByIdsOrSearch(realm.getId(), rawOrExtractedIds, search, first, max)
                .stream()
                .map(groupEntity -> new GroupAdapter(session, realm, groupEntity, groupRepository, roleRepository, this.model));
    }

    @Override
    public Long getGroupsCount(RealmModel realm, Boolean onlyTopGroups) {
        logger.info("Getting groups count: realm={}, onlyTopGroups={}", realm.getId(), onlyTopGroups);
        if (Boolean.TRUE.equals(onlyTopGroups)) {
            return groupRepository.countTopLevelGroups(realm.getId());
        }
        return groupRepository.countGroups(realm.getId());
    }

    @Override
    public Long getGroupsCountByNameContaining(RealmModel realm, String search) {
        logger.info("Getting groups count by name: realm={}, search={}", realm.getId(), search);
        return groupRepository.countGroupsByName(realm.getId(), search);
    }

    @Override
    public Stream<GroupModel> searchGroupsByAttributes(RealmModel realm, Map<String, String> attributes, Integer firstResult, Integer maxResults) {
        logger.info("Searching groups by attributes: realm={}, attributes={}, first={}, max={}", realm.getId(), attributes, firstResult, maxResults);
        logger.warn("searchGroupsByAttributes - GroupRepository.searchByAttributes does not currently filter by realm.");
        return groupRepository.searchByAttributes(attributes, firstResult, maxResults)
                .stream()
                .map(groupEntity -> new GroupAdapter(session, realm, groupEntity, groupRepository, roleRepository, this.model));
    }

    @Override
    public Stream<GroupModel> searchForGroupByNameStream(RealmModel realm, String search, Boolean exact, Integer firstResult, Integer maxResults) {
        logger.info("Searching for group by name: realm={}, search={}, exact={}, first={}, max={}", realm.getId(), search, exact, firstResult, maxResults);
        return groupRepository.searchGroupsByName(realm.getId(), search, exact, firstResult, maxResults)
                .stream()
                .map(groupEntity -> new GroupAdapter(session, realm, groupEntity, groupRepository, roleRepository, this.model));
    }

    @Override
    public Stream<GroupModel> getGroupsByRoleStream(RealmModel realm, RoleModel role, Integer firstResult, Integer maxResults) {
        String roleId = StorageId.externalId(role.getId());
        logger.info("Getting groups by role: realm={}, roleId={}, first={}, max={}", realm.getId(), roleId, firstResult, maxResults);
        return groupRepository.findGroupsByRoleId(realm.getId(), roleId, firstResult, maxResults)
                .stream()
                .map(groupEntity -> new GroupAdapter(session, realm, groupEntity, groupRepository, roleRepository, this.model));
    }

    @Override
    public Stream<GroupModel> getTopLevelGroupsStream(RealmModel realm, String search, Boolean exact, Integer firstResult, Integer maxResults) {
        logger.info("Getting top-level groups stream: realm={}, search={}, exact={}, first={}, max={}", realm.getId(), search, exact, firstResult, maxResults);
        return groupRepository.getTopLevelGroups(realm.getId(), search, exact, firstResult, maxResults)
                .stream()
                .map(groupEntity -> new GroupAdapter(session, realm, groupEntity, groupRepository, roleRepository, this.model));
    }

    @Override
    public GroupModel createGroup(RealmModel realm, String id, String name, GroupModel toParent) {
        String parentId = (toParent != null) ? StorageId.externalId(toParent.getId()) : null;
        logger.info("Creating group: realm={}, keycloakProposedId={}, name={}, parentId={}", realm.getId(), id, name, parentId);

        if (groupRepository.findGroupByNameAndParent(realm.getId(), name, parentId) != null) {
            logger.warn("Group with name '{}' already exists under parent {} (or at top level) in realm {}", name, (toParent != null ? toParent.getName() : "top-level"), realm.getId());
            return null;
        }

        Group groupEntity = new Group();
        groupEntity.setKeycloakId(id);
        groupEntity.setName(name);
        groupEntity.setRealmId(realm.getId());

        if (parentId != null) {
            long parentIdLong;
            try {
                parentIdLong = Long.parseLong(parentId);
            } catch (NumberFormatException e) {
                logger.warn("Invalid parent group ID format for creation: {}", parentId);
                return null;
            }
            groupEntity.setParentId(parentIdLong);
        }

        try {
            groupRepository.save(groupEntity);

            GroupModel newGroupModel = new GroupAdapter(session, realm, groupEntity, groupRepository, roleRepository, model);

            String keycloakGroupId = newGroupModel.getId();
            if (keycloakGroupId != null) {
                Group managedGroup = groupRepository.getById(String.valueOf(groupEntity.getId()));
                if (managedGroup != null) {
                    managedGroup.setKeycloakId(keycloakGroupId);
                    groupRepository.save(managedGroup);
                    logger.info("Saved Keycloak ID {} for group {}", keycloakGroupId, groupEntity.getName());
                } else {
                    logger.warn("Could not re-fetch group entity by ID {} to save Keycloak ID {}", groupEntity.getId(), keycloakGroupId);
                }
            } else {
                logger.warn("Keycloak did not provide a UUID for newly created group {}", groupEntity.getName());
            }

            logger.info("Successfully created group: {} with external id {} and keycloak id {}", groupEntity.getName(), groupEntity.getId(), groupEntity.getKeycloakId());
            return newGroupModel;
        } catch (Exception e) {
            logger.error("Failed to create group {} in realm {}: {}", name, realm.getId(), e.getMessage(), e);
            return null;
        }
    }

    @Override
    public boolean removeGroup(RealmModel realm, GroupModel groupModel) {
        if (groupModel == null) return false;
        String keycloakGroupId = groupModel.getId();
        logger.info("Removing group: realm={}, keycloakGroupId={}", realm.getId(), keycloakGroupId);

        try {
            groupRepository.delete(keycloakGroupId);
            logger.info("Successfully triggered deletion for group with Keycloak ID: {}", keycloakGroupId);
            return true;
        } catch (Exception e) {
            logger.error("Failed to delete group with Keycloak ID {}: {}", keycloakGroupId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void moveGroup(RealmModel realm, GroupModel groupModel, GroupModel toParent) {
        String keycloakGroupId = groupModel.getId();
        String keycloakParentId = (toParent != null) ? toParent.getId() : null;
        logger.info("Moving group: realm={}, keycloakGroupId={}, keycloakParentId={}", realm.getId(), keycloakGroupId, keycloakParentId);

        Group groupEntity = groupRepository.getById(keycloakGroupId);

        if (groupEntity == null) {
            logger.warn("Group entity not found for move operation with Keycloak ID: {}", keycloakGroupId);
            return;
        }

        Long newParentIdLong = null;
        if (keycloakParentId != null) {
            Group parentEntity = groupRepository.getById(keycloakParentId);
            if (parentEntity != null) {
                newParentIdLong = parentEntity.getId();
                logger.info("Resolved parent Keycloak ID {} to external ID {}", keycloakParentId, newParentIdLong);
            } else {
                logger.warn("Target parent group entity not found for Keycloak ID: {}. Cannot move.", keycloakParentId);
                return;
            }
        }

        groupEntity.setParentId(newParentIdLong);
        // TODO: Implement parentPath update logic if managed in entity
        logger.warn("moveGroup - parentPath update logic not implemented.");
        if (newParentIdLong == null) {
            groupEntity.setParentPath(null);
        }

        try {
            groupRepository.save(groupEntity);
            logger.info("Successfully moved group with Keycloak ID {} to parent {}", keycloakGroupId, newParentIdLong != null ? String.valueOf(newParentIdLong) : "top-level");
        } catch (Exception e) {
            logger.error("Failed to move group with Keycloak ID {}: {}", keycloakGroupId, e.getMessage(), e);
        }
    }

    @Override
    public void addTopLevelGroup(RealmModel realm, GroupModel subGroupModel) {
        String keycloakGroupId = subGroupModel.getId();
        logger.info("Adding group to top level: realm={}, keycloakGroupId={}", realm.getId(), keycloakGroupId);

        Group groupEntity = groupRepository.getById(keycloakGroupId);

        if (groupEntity == null) {
            logger.warn("Group entity not found for adding to top level with Keycloak ID: {}", keycloakGroupId);
            return;
        }

        groupEntity.setParentId(null);
        groupEntity.setParentPath(null);

        try {
            groupRepository.save(groupEntity);
            logger.info("Successfully moved group with Keycloak ID {} to top level", keycloakGroupId);
        } catch (Exception e) {
            logger.error("Failed to add group with Keycloak ID {} to top level: {}", keycloakGroupId, e.getMessage(), e);
        }
    }

    // Note: Role mapping methods and user membership methods are implemented in the adapter.

    // Provider-level cleanup methods would be here.

}