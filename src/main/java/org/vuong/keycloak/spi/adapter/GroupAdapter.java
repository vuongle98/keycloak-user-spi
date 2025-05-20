package org.vuong.keycloak.spi.adapter;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.*;
import org.keycloak.storage.StorageId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vuong.keycloak.spi.entity.Group;
import org.vuong.keycloak.spi.entity.Role;
import org.vuong.keycloak.spi.repository.GroupRepository;
import org.vuong.keycloak.spi.repository.RoleRepository;

import java.util.*;
import java.util.stream.Stream;
import java.util.Objects;

// Implement GroupModel directly
public class GroupAdapter implements GroupModel {

    private static final Logger logger = LoggerFactory.getLogger(GroupAdapter.class);

    private final KeycloakSession session;
    private final RealmModel realm;
    private final Group groupEntity;
    private final GroupRepository groupRepository;
    private final RoleRepository roleRepository;
    private final ComponentModel storageProviderModel;

    // Constructor needs necessary dependencies
    public GroupAdapter(KeycloakSession session, RealmModel realm, Group groupEntity, GroupRepository groupRepository, RoleRepository roleRepository, ComponentModel storageProviderModel) {
        this.session = session;
        this.realm = realm;
        this.groupEntity = groupEntity;
        this.groupRepository = groupRepository;
        this.roleRepository = roleRepository;
        this.storageProviderModel = storageProviderModel;
    }

    // --- Methods from GroupModel interface (as per your provided definition) ---

    @Override // Abstract method
    public String getId() {
        return StorageId.keycloakId(storageProviderModel, String.valueOf(groupEntity.getId()));
    }

    @Override // Abstract method
    public String getName() {
        return groupEntity.getName();
    }

    @Override // Abstract method
    public void setName(String name) {
        groupEntity.setName(name);
        groupRepository.save(groupEntity);
    }

    @Override // Abstract method
    public void setSingleAttribute(String name, String value) {
        logger.info("Setting single attribute '{}' for group {}", name, getId());
        Map<String, List<String>> attributes = getAttributes();
        if (attributes == null) {
            attributes = new MultivaluedHashMap<>();
        }
        attributes.put(name, value != null ? Collections.singletonList(value) : Collections.emptyList());
        setAttributes(attributes); // Call setAttributes to persist
    }

    @Override // Abstract method
    public void setAttribute(String name, List<String> values) {
        logger.info("Setting attribute '{}' for group {}", name, getId());
        Map<String, List<String>> attributes = getAttributes();
        if (attributes == null) {
            attributes = new MultivaluedHashMap<>();
        }
        attributes.put(name, values != null ? values : Collections.emptyList());
        setAttributes(attributes); // Call setAttributes to persist
    }

    @Override // Abstract method
    public void removeAttribute(String name) {
        logger.info("Removing attribute '{}' for group {}", name, getId());
        Map<String, List<String>> attributes = getAttributes();
        if (attributes != null) {
            attributes.remove(name);
            setAttributes(attributes); // Call setAttributes to persist
        }
    }

    public void setAttributes(Map<String, List<String>> attributes) {
        logger.info("Setting attributes for group {}", getId());

        if (attributes == null) {
            // If Keycloak passes null, you might clear all attributes or do nothing
            // depending on your desired behavior.
            // Example: Clear description if it exists
            // groupEntity.setDescription(null);
            // If using a separate attribute table, delete all entries for this group.
            logger.debug("Received null attributes map for group {}. Clearing all attributes.", getId());
            // Example: If attributes are stored in a separate table, you'd call a repository method here:
            // attributeRepository.deleteAllAttributesForGroup(groupEntity.getId());
            groupRepository.save(groupEntity); // Save the entity after changes
            return;
        }

        // Iterate through the received attributes map
        for (Map.Entry<String, List<String>> entry : attributes.entrySet()) {
            String attributeName = entry.getKey();
            List<String> attributeValues = entry.getValue();

            // Implement logic to map attributeName to your entity fields
            switch (attributeName) {
                case "code":
                    if (attributeValues != null && !attributeValues.isEmpty()) {
                        groupEntity.setCode(attributeValues.get(0));
                    } else {
                        groupEntity.setCode(null);
                    }
                    break;
                case "name":
                    if (attributeValues != null && !attributeValues.isEmpty()) {
                        groupEntity.setName(attributeValues.get(0));
                    } else {
                        groupEntity.setName(null);
                    }
                    break;
                case "description":
                    // Assuming 'description' is a single-valued string in your entity
                    if (attributeValues != null && !attributeValues.isEmpty()) {
                        // Set the first value from the list
                        groupEntity.setDescription(attributeValues.get(0));
                    } else {
                        // If the value list is empty or null, clear the description
                        groupEntity.setDescription(null);
                    }
                    break;
                // Add cases for other attributes you want to support mapping
                default:
                    // Handle unknown attributes. You might log a warning, ignore them,
                    // or store them in a generic attribute table if your schema supports it.
                    logger.warn("Attempted to set unsupported attribute '{}' for group {}", attributeName, getId());
                    break;
            }
        }

        // After updating the entity fields based on the map, save the entity
        // If using a separate attribute table, you might save the main entity first
        // and then perform batch updates/inserts/deletes on the attribute table.
        try {
            groupRepository.save(groupEntity); // Assumes save handles updates to the existing entity
            logger.info("Successfully set attributes for group {}", getId());
        } catch (Exception e) {
            logger.error("Failed to save group {} after setting attributes: {}", getId(), e.getMessage(), e);
            // Handle the exception appropriately
        }
    }

    @Override // Abstract method
    public String getFirstAttribute(String name) {
        Map<String, List<String>> attributes = getAttributes();
        List<String> values = attributes != null ? attributes.get(name) : null;
        return values != null && !values.isEmpty() ? values.get(0) : null;
    }

    @Override // Abstract method
    public Stream<String> getAttributeStream(String name) {
        List<String> values = getAttribute(name);
        return values != null ? values.stream() : Stream.empty();
    }

    @Override // Abstract method
    public Map<String, List<String>> getAttributes() {
        logger.info("Getting attributes for group {}", getId());
        MultivaluedHashMap<String, String> attributes = new MultivaluedHashMap<>();
        // Map fields from your Group entity or related attribute entities to Keycloak attributes.
        // Example: attributes.add("description", groupEntity.getDescription()); // If Group entity has a description field
        return attributes;
    }

    @Override // Abstract method
    public GroupModel getParent() {
        logger.debug("GroupAdapter.getParent() for group {}", getName());
        if (groupEntity.getParentId() == null) {
            return null;
        }
        Group parentEntity = groupRepository.getById(String.valueOf(groupEntity.getParentId()));
        if (parentEntity != null) {
            return new GroupAdapter(session, realm, parentEntity, groupRepository, roleRepository, storageProviderModel);
        }
        return null;
    }

    @Override // Abstract method
    public String getParentId() {
        return groupEntity.getParentId() != null ? String.valueOf(groupEntity.getParentId()) : null;
    }

    @Override // Abstract method
    public Stream<GroupModel> getSubGroupsStream() {
        logger.debug("GroupAdapter.getSubGroupsStream() for group {}", getName());
        // Requires a method in GroupRepository to find groups by parentId returning Stream or List
        // return groupRepository.findGroupsByParentIdStream(realm.getId(), groupEntity.getId()) // Needs implementation
        //         .map(sub -> new GroupAdapter(session, realm, sub, groupRepository, roleRepository, storageProviderModel)); // Create adapter for each subgroup
        logger.warn("getSubGroupsStream() not fully implemented in GroupAdapter.");
        return Stream.empty(); // Placeholder
    }

    // Note: Default methods for getSubGroupsStream(search, first, max) and getSubGroupsCount()
    // are provided in the interface itself and call the abstract getSubGroupsStream().
    // You don't need to reimplement them unless providing a more efficient query.

    @Override // Abstract method
    public void setParent(GroupModel group) {
        logger.debug("GroupAdapter.setParent({}) for group {}", group != null ? group.getName() : "null", getName());
        Long newParentId = null;
        if (group != null) {
            String newParentKeycloakId = group.getId();
            Group newParentEntity = groupRepository.getById(newParentKeycloakId);
            if (newParentEntity != null) {
                newParentId = newParentEntity.getId();
            } else {
                logger.warn("New parent group entity not found for Keycloak ID: {}. Cannot set parent.", newParentKeycloakId);
                return;
            }
        }
        groupEntity.setParentId(newParentId);
        // TODO: Update parentPath if managed in entity
        groupRepository.save(groupEntity);
    }

    @Override // Abstract method
    public void addChild(GroupModel subGroup) {
        logger.debug("GroupAdapter.addChild({}) for group {}", subGroup != null ? subGroup.getName() : "null", getName());
        logger.info("GroupAdapter.addChild called, relationship should be handled by setParent on subgroup.");
    }

    @Override // Abstract method
    public void removeChild(GroupModel subGroup) {
        logger.debug("GroupAdapter.removeChild({}) for group {}", subGroup != null ? subGroup.getName() : "null", getName());
        logger.info("GroupAdapter.removeChild called, relationship should be handled by setParent on subgroup.");
    }

    // New methods from the second GroupModel definition you provided:
    // Removed @Override as these are likely default methods you are implementing
    public boolean escapeSlashesInGroupPath() {
        logger.debug("GroupAdapter.escapeSlashesInGroupPath()");
        return false; // Default implementation
    }

    // --- Methods from RoleMapperModel interface (inherited by GroupModel) ---
    // These methods must also be implemented.

    @Override // Abstract method
    public void grantRole(RoleModel roleModel) {
        String roleId = StorageId.externalId(roleModel.getId());
        logger.info("Granting role {} to group {}", roleId, getId());

        Role roleEntity = roleRepository.getById(roleId);

        if (roleEntity != null) {
            if (groupEntity.getRoles() == null) {
                groupEntity.setRoles(new HashSet<>());
            }
            groupEntity.getRoles().add(roleEntity);
            groupRepository.save(groupEntity);
            logger.info("Successfully granted role {} to group {}", roleId, getId());
        } else {
            logger.warn("Role not found with ID {} for granting to group {}", roleId, getId());
        }
    }

    @Override // Abstract method
    public void deleteRoleMapping(RoleModel roleModel) {
        String roleId = StorageId.externalId(roleModel.getId());
        logger.info("Removing role {} from group {}", roleId, getId());

        Role roleEntity = roleRepository.getById(roleId);

        if (roleEntity != null && groupEntity.getRoles() != null) {
            boolean removed = groupEntity.getRoles().removeIf(r -> Objects.equals(r.getId(), roleEntity.getId()));
            if (removed) {
                groupRepository.save(groupEntity);
                logger.info("Successfully removed role {} from group {}", roleId, getId());
            } else {
                logger.warn("Role with ID {} not found in group {} roles for removal.", roleId, getId());
            }
        } else {
            logger.warn("Role with ID {} not found or group {} has no roles for removal.", roleId, getId());
        }
    }

    @Override // Abstract method (from RoleMapperModel, likely)
    public Stream<RoleModel> getRoleMappingsStream() {
        logger.debug("GroupAdapter.getRoleMappingsStream() for group {}", getId());
        if (groupEntity.getRoles() != null) {
            return groupEntity.getRoles().stream()
                    .map(role -> new RoleAdapter(session, realm, storageProviderModel, role, roleRepository));
        }
        return Stream.empty();
    }


    // Using !role.isClientRole() based on the RoleModel definition you provided previously
    @Override // Abstract method (from RoleMapperModel, likely)
    public Stream<RoleModel> getRealmRoleMappingsStream() {
        logger.debug("GroupAdapter.getRealmRoleMappingsStream() for group {}", getId());
        return getRoleMappingsStream()
                .filter(role -> !role.isClientRole()); // Filter for realm roles using !isClientRole()
    }

    @Override // Abstract method (from RoleMapperModel, likely)
    public Stream<RoleModel> getClientRoleMappingsStream(ClientModel client) {
        String clientId = client.getId();
        return getRoleMappingsStream()
                .filter(role -> role.isClientRole() && Objects.equals(role.getContainerId(), clientId));
    }

    @Override // Abstract method (from RoleMapperModel, likely)
    public boolean hasRole(RoleModel role) {
        logger.debug("GroupAdapter.hasRole({}) for group {}", role != null ? role.getName() : "null", getName());
        if (role == null) return false;

        // Option 1: Check direct role mappings
        if (hasDirectRole(role)) {
            return true;
        }

        // Option 2: Check roles inherited from parent groups (if applicable)
        // Implement logic here if needed.

        return false; // Only direct roles checked by hasDirectRole
    }

    @Override // Abstract method (from RoleMapperModel, likely)
    public boolean hasDirectRole(RoleModel role) {
        logger.debug("GroupAdapter.hasDirectRole({}) for group {}", role != null ? role.getName() : "null", getName());
        if (role == null || groupEntity.getRoles() == null) {
            return false;
        }
        String roleId = StorageId.externalId(role.getId());
        Role roleEntity = roleRepository.getById(roleId);

        if (roleEntity != null) {
            return groupEntity.getRoles().contains(roleEntity);
        }
        return false;
    }

    // --- Other methods typically needed by adapters ---
    // Implement these as needed based on your entity fields

    public List<String> getAttribute(String name) {
        Map<String, List<String>> attributes = getAttributes();
        return attributes != null ? attributes.get(name) : null;
    }


    // Method to access the wrapped entity (optional, but can be useful)
    public Group getWrappedEntity() {
        return groupEntity;
    }

    // Override equals, hashCode, compareTo for correctness
    @Override // From Object or Comparable
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GroupModel)) return false;
        GroupModel that = (GroupModel) o;

        String thisId = this.getId();
        String thatId = that.getId();
        return Objects.equals(thisId, thatId);
    }

    @Override // From Object
    public int hashCode() {
        String id = getId();
        return Objects.hashCode(id);
    }

}