package org.vuong.keycloak.spi.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.keycloak.models.GroupModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vuong.keycloak.spi.entity.Group;
import org.vuong.keycloak.spi.entity.Role;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID; // Import UUID for validation
import java.util.regex.Pattern; // Optional: For simple UUID format check
import java.util.stream.Collectors;


public class GroupRepository {

    private static final Logger log = LoggerFactory.getLogger(GroupRepository.class);
    private final EntityManager em;

    // Optional: Pre-compile UUID pattern for potential format check
    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");


    public GroupRepository(EntityManager em) {
        this.em = em;
    }

    public Group getById(String id) {
        log.debug("GroupRepository.getById({})", id);
        if (id == null || id.trim().isEmpty()) {
            return null;
        }

        // Try to parse as Long (external ID)
        try {
            Long groupIdLong = Long.valueOf(id);
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Group> cq = cb.createQuery(Group.class);
            Root<Group> root = cq.from(Group.class);
            cq.where(cb.equal(root.get("id"), groupIdLong)); // Query by external Long ID
            return em.createQuery(cq).getSingleResult();
        } catch (NumberFormatException e) {
            // If it's not a Long, it might be a Keycloak UUID or composite ID
            log.debug("Group ID {} is not a valid Long format. Trying as Keycloak ID.", id);
            // Fall through to check if it's a Keycloak ID (UUID)
        }

        // If not found by Long ID, try to find by Keycloak ID (UUID)
        // Note: StorageId.externalId(id) should extract the Long string from composite ID.
        // If Keycloak passes raw UUID, StorageId.externalId will likely return null.
        // So, we'll directly use the received 'id' to look up by Keycloak ID.
        // Add a check to see if the string looks like a UUID before querying by keycloakId.
        if (isUUID(id)) { // Use helper method or try-catch UUID.fromString
            log.debug("Group ID {} looks like a UUID. Attempting lookup by Keycloak ID.", id);
            return findByKeycloakId(id); // New method
        } else {
            // If it's not a Long and doesn't look like a UUID (might be composite or something else)
            // In case of composite ID like "provider_alias.external_id", StorageId.externalId should handle extraction.
            // The initial NumberFormatException suggests it wasn't a Long *after* extraction or wasn't extracted.
            // This scenario might need further debugging based on the actual ID format Keycloak sends.
            log.warn("Group ID {} is not a Long and doesn't look like a UUID. Cannot perform lookup by ID format.", id);
            return null; // ID format not recognized
        }
    }

    // Helper method to check if a string is a valid UUID format
    private boolean isUUID(String id) {
        if (id == null || id.length() != 36) { // Standard UUID length with hyphens
            return false;
        }
        // Simple regex check or try-catch UUID.fromString
        return UUID_PATTERN.matcher(id).matches();
//         try {
//             UUID.fromString(id);
//             return true;
//         } catch (IllegalArgumentException e) {
//             return false;
//         }
    }


    public Group findByName(String name) {
        log.debug("GroupRepository.findByName({}) - generic find (not realm/parent filtered)", name);
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Group> cq = cb.createQuery(Group.class);
            Root<Group> root = cq.from(Group.class);
            cq.where(cb.equal(root.get("name"), name));
            return em.createQuery(cq).getSingleResult();
        } catch (NoResultException e) {
            log.debug("Group not found with name: {}", name);
            return null;
        }
    }

    // Implemented based on CustomUserStorageProvider needs
    public Group findGroupByNameAndParent(String realmId, String name, String parentId) {
        log.debug("GroupRepository.findGroupByNameAndParent(realmId={}, name={}, parentId={})", realmId, name, parentId);
        if (realmId == null || realmId.trim().isEmpty() || name == null || name.trim().isEmpty()) {
            return null;
        }
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Group> cq = cb.createQuery(Group.class);
        Root<Group> root = cq.from(Group.class);
        List<Predicate> predicates = new ArrayList<>();

        predicates.add(cb.equal(root.get("realmId"), realmId)); // Assuming Group has realmId
        predicates.add(cb.equal(root.get("name"), name));

        if (parentId == null || parentId.trim().isEmpty()) {
            // Searching for a top-level group
            predicates.add(cb.isNull(root.get("parentId")));
        } else {
            // Searching for a subgroup with a specific parent
            try {
                Long parentIdLong = Long.valueOf(parentId); // Parent ID is expected to be a Long string from Keycloak's externalId
                predicates.add(cb.equal(root.get("parentId"), parentIdLong));
            } catch (NumberFormatException e) {
                log.warn("Invalid parent group ID format for findGroupByNameAndParent: {}", parentId, e);
                return null; // Invalid parent ID format means group cannot exist
            }
        }

        cq.where(cb.and(predicates.toArray(new Predicate[0])));

        try {
            return em.createQuery(cq).getSingleResult();
        } catch (NoResultException e) {
            log.debug("Group not found with name {} and parentId {} in realm {}", name, parentId, realmId);
            return null;
        }
    }

    // Add this new method to find a group by its Keycloak UUID
    public Group findByKeycloakId(String keycloakId) {
        log.debug("GroupRepository.findByKeycloakId({})", keycloakId);
        if (keycloakId == null || keycloakId.trim().isEmpty()) {
            return null;
        }
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Group> cq = cb.createQuery(Group.class);
            Root<Group> root = cq.from(Group.class);
            cq.where(cb.equal(root.get("keycloakId"), keycloakId)); // Query by Keycloak UUID
            return em.createQuery(cq).getSingleResult();
        } catch (NoResultException e) {
            log.debug("Group not found with keycloakId: {}", keycloakId);
            return null;
        }
    }


    public List<Group> search(String keyword) {
        log.debug("GroupRepository.search({}) - generic search (not realm filtered)", keyword);
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Group> cq = cb.createQuery(Group.class);
        Root<Group> root = cq.from(Group.class);

        if (keyword != null && !keyword.isEmpty()) {
            cq.where(cb.like(cb.lower(root.get("name")), "%" + keyword.toLowerCase() + "%"));
        }

        return em.createQuery(cq).getResultList();
    }

    public List<Group> getAllGroups() {
        log.debug("GroupRepository.getAllGroups() - generic get all (not realm/pagination filtered)");
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Group> cq = cb.createQuery(Group.class);
        cq.from(Group.class);
        return em.createQuery(cq).getResultList();
    }

    // Implemented based on CustomUserStorageProvider needs (with realm and pagination)
    public List<Group> getAllGroups(String realmId, Integer firstResult, Integer maxResults) {
        log.debug("GroupRepository.getAllGroups(realmId={}, first={}, max={})", realmId, firstResult, maxResults);
        if (realmId == null || realmId.trim().isEmpty()) {
            return new ArrayList<>();
        }
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Group> cq = cb.createQuery(Group.class);
        Root<Group> root = cq.from(Group.class);
        List<Predicate> predicates = new ArrayList<>();

        // Filter by realm
        predicates.add(cb.equal(root.get("realmId"), realmId)); // Assuming Group has realmId

        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        // Optional: Add ordering, e.g., cq.orderBy(cb.asc(root.get("name")));

        TypedQuery<Group> query = em.createQuery(cq);

        if (firstResult != null) {
            query.setFirstResult(firstResult);
        }
        if (maxResults != null) {
            query.setMaxResults(maxResults);
        }

        return query.getResultList();
    }


    // Implemented based on CustomUserStorageProvider needs
    public List<Group> getGroupsByIdsOrSearch(String realmId, List<String> groupIds, String search, Integer first, Integer max) {
        log.debug("GroupRepository.getGroupsByIdsOrSearch(realmId={}, groupIds={}, search={}, first={}, max={})", realmId, groupIds, search, first, max);
        if (realmId == null || realmId.trim().isEmpty()) {
            return new ArrayList<>();
        }
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Group> cq = cb.createQuery(Group.class);
        Root<Group> root = cq.from(Group.class);
        List<Predicate> predicates = new ArrayList<>();

        // Filter by realm
        predicates.add(cb.equal(root.get("realmId"), realmId)); // Assuming Group has realmId

        List<Long> groupIdsLong = new ArrayList<>();
        List<String> keycloakGroupIds = new ArrayList<>();

        if (groupIds != null && !groupIds.isEmpty()) {
            for (String id : groupIds) {
                // Attempt to parse as Long (external ID)
                try {
                    groupIdsLong.add(Long.valueOf(id));
                } catch (NumberFormatException e) {
                    // If it's not a Long, it might be a Keycloak UUID or composite ID
                    log.debug("Group ID {} in list is not a valid Long format. Adding to Keycloak ID list.", id);
                    // Assume it's a Keycloak ID (UUID or composite) and add to a separate list
                    keycloakGroupIds.add(id);
                }
            }
        }

        List<Predicate> idPredicates = new ArrayList<>();
        if (!groupIdsLong.isEmpty()) {
            idPredicates.add(root.get("id").in(groupIdsLong)); // Query by external Long IDs
        }
        if (!keycloakGroupIds.isEmpty()) {
            // Query by Keycloak UUIDs (need findByKeycloakId logic) or composite IDs
            // For composite IDs, need to extract external ID.
            // For raw UUIDs, need to query keycloakId column.
            // The simplest is to add predicates for both cases if the IDs might be mixed types in the list.
            List<String> extractedExternalIds = new ArrayList<>();
            List<String> rawKeycloakIds = new ArrayList<>();

            for (String kId : keycloakGroupIds) {
                String extracted = org.keycloak.storage.StorageId.externalId(kId);
                if (extracted != null) {
                    extractedExternalIds.add(extracted); // Add extracted external ID string
                } else if (isUUID(kId)) { // Check if the raw ID looks like a UUID
                    rawKeycloakIds.add(kId); // Add raw UUID
                } else {
                    log.warn("Group ID {} in list not recognized as Long, composite, or UUID.", kId);
                }
            }

            // Add predicates for extracted external IDs (converted to Long)
            if (!extractedExternalIds.isEmpty()) {
                try {
                    List<Long> extractedLongIds = extractedExternalIds.stream().map(Long::valueOf).collect(Collectors.toList());
                    idPredicates.add(root.get("id").in(extractedLongIds));
                } catch (NumberFormatException e) {
                    log.warn("Error converting extracted external IDs to Long: {}", extractedExternalIds, e);
                }
            }
            // Add predicates for raw Keycloak UUIDs (query keycloakId column)
            if (!rawKeycloakIds.isEmpty()) {
                idPredicates.add(root.get("keycloakId").in(rawKeycloakIds)); // Query by Keycloak UUID
            }
        }


        if (search != null && !search.trim().isEmpty()) {
            Predicate searchPredicate = cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase() + "%");

            if (!idPredicates.isEmpty()) {
                // If both IDs/Keycloak IDs and search are provided, combine with OR
                predicates.add(cb.or(cb.or(idPredicates.toArray(new Predicate[0])), searchPredicate));
            } else {
                // Only search criteria
                predicates.add(searchPredicate);
            }
        } else if (!idPredicates.isEmpty()) {
            // Only IDs/Keycloak IDs provided
            predicates.add(cb.or(idPredicates.toArray(new Predicate[0])));
        } else {
            // No IDs and no search term, return empty list as per Keycloak behavior expectation
            return new ArrayList<>();
        }


        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        // Optional: Add ordering

        TypedQuery<Group> query = em.createQuery(cq);

        if (first != null) {
            query.setFirstResult(first);
        }
        if (max != null) {
            query.setMaxResults(max);
        }

        return query.getResultList();
    }


    // Implemented based on CustomUserStorageProvider needs
    public Long countGroups(String realmId) {
        log.debug("GroupRepository.countGroups(realmId={})", realmId);
        if (realmId == null || realmId.trim().isEmpty()) {
            return 0L;
        }
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Long> cq = cb.createQuery(Long.class);
            Root<Group> root = cq.from(Group.class);
            List<Predicate> predicates = new ArrayList<>();

            // Filter by realm
            predicates.add(cb.equal(root.get("realmId"), realmId)); // Assuming Group has realmId

            cq.select(cb.count(root)).where(cb.and(predicates.toArray(new Predicate[0])));
            return em.createQuery(cq).getSingleResult();
        } catch (NoResultException e) {
            return 0L;
        }
    }

    // Implemented based on CustomUserStorageProvider needs
    public Long countTopLevelGroups(String realmId) {
        log.debug("GroupRepository.countTopLevelGroups(realmId={})", realmId);
        if (realmId == null || realmId.trim().isEmpty()) {
            return 0L;
        }
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Long> cq = cb.createQuery(Long.class);
            Root<Group> root = cq.from(Group.class);
            List<Predicate> predicates = new ArrayList<>();

            // Filter by realm
            predicates.add(cb.equal(root.get("realmId"), realmId)); // Assuming Group has realmId
            // Filter for top-level groups (parentId is null)
            predicates.add(cb.isNull(root.get("parentId")));

            cq.select(cb.count(root)).where(cb.and(predicates.toArray(new Predicate[0])));
            return em.createQuery(cq).getSingleResult();
        } catch (NoResultException e) {
            return 0L;
        }
    }

    // Implemented based on CustomUserStorageProvider needs
    public Long countGroupsByName(String realmId, String search) {
        log.debug("GroupRepository.countGroupsByName(realmId={}, search={})", realmId, search);
        if (realmId == null || realmId.trim().isEmpty()) {
            return 0L;
        }
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Long> cq = cb.createQuery(Long.class);
            Root<Group> root = cq.from(Group.class);
            List<Predicate> predicates = new ArrayList<>();

            // Filter by realm
            predicates.add(cb.equal(root.get("realmId"), realmId)); // Assuming Group has realmId

            if (search != null && !search.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase() + "%"));
            }

            cq.select(cb.count(root)).where(cb.and(predicates.toArray(new Predicate[0])));
            return em.createQuery(cq).getSingleResult();
        } catch (NoResultException e) {
            return 0L;
        }
    }


    public List<Group> searchByAttributes(Map<String, String> attributes, Integer firstResult, Integer maxResults) {
        log.debug("GroupRepository.searchByAttributes(attributes={}, first={}, max={}) - (not realm filtered)", attributes, firstResult, maxResults);
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Group> query = cb.createQuery(Group.class);
        Root<Group> root = query.from(Group.class);

        List<Predicate> predicates = new ArrayList<>();

        attributes.forEach((key, value) -> {
            if (value != null && !value.isEmpty()) {
                // Note: This assumes attribute keys directly map to entity field names.
                // Consider potential security implications if user input controls field names.
                // TODO: Add realm filtering here if necessary: predicates.add(cb.equal(root.get("realmId"), realmId)); // Assuming Group has realmId
                predicates.add(cb.like(cb.lower(root.get(key)), "%" + value.toLowerCase() + "%"));
            }
        });

        query.select(root).where(predicates.toArray(new Predicate[0]));

        TypedQuery<Group> typedQuery = em.createQuery(query);
        if (firstResult != null) typedQuery.setFirstResult(firstResult);
        if (maxResults != null) typedQuery.setMaxResults(maxResults);

        return typedQuery.getResultList();
    }

    // Implemented based on CustomUserStorageProvider needs (with realm, exact, pagination)
    public List<Group> searchGroupsByName(String realmId, String search, Boolean exact, Integer firstResult, Integer maxResults) {
        log.debug("GroupRepository.searchGroupsByName(realmId={}, search={}, exact={}, first={}, max={})", realmId, search, exact, firstResult, maxResults);
        if (realmId == null || realmId.trim().isEmpty() || search == null || search.trim().isEmpty()) {
            return new ArrayList<>();
        }
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Group> cq = cb.createQuery(Group.class);
        Root<Group> root = cq.from(Group.class);
        List<Predicate> predicates = new ArrayList<>();

        // Filter by realm
        predicates.add(cb.equal(root.get("realmId"), realmId)); // Assuming Group has realmId

        if (Boolean.TRUE.equals(exact)) {
            predicates.add(cb.equal(root.get("name"), search));
        } else {
            predicates.add(cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase() + "%"));
        }

        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        // Optional: Add ordering

        TypedQuery<Group> query = em.createQuery(cq);

        if (firstResult != null) {
            query.setFirstResult(firstResult);
        }
        if (maxResults != null) {
            query.setMaxResults(maxResults);
        }

        return query.getResultList();
    }


    // Implemented based on CustomUserStorageProvider needs
    public List<Group> findGroupsByRoleId(String realmId, String roleId, Integer firstResult, Integer maxResults) {
        log.debug("GroupRepository.findGroupsByRoleId(realmId={}, roleId={}, first={}, max={})", realmId, roleId, firstResult, maxResults);
        if (realmId == null || realmId.trim().isEmpty() || roleId == null || roleId.trim().isEmpty()) {
            return new ArrayList<>();
        }
        // Role IDs from Keycloak are expected to be composite IDs or external IDs (Long strings)
        Long roleIdLong;
        try {
            roleIdLong = Long.valueOf(org.keycloak.storage.StorageId.externalId(roleId)); // Extract external ID and parse
        } catch (NumberFormatException e) {
            log.warn("Invalid role ID format (after extraction) for findGroupsByRoleId: {}", roleId, e);
            return new ArrayList<>(); // Invalid role ID format
        } catch (IllegalArgumentException e) {
            log.warn("Could not extract external ID from roleId for findGroupsByRoleId: {}", roleId, e);
            return new ArrayList<>(); // Invalid composite ID format
        }


        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Group> cq = cb.createQuery(Group.class);
        Root<Group> root = cq.from(Group.class);
        Join<Group, Role> rolesJoin = root.join("roles"); // Join the many-to-many roles relationship

        List<Predicate> predicates = new ArrayList<>();

        // Filter by realm (assuming Group has realmId)
        predicates.add(cb.equal(root.get("realmId"), realmId)); // Assuming Group has realmId
        // Filter by role ID in the joined table
        predicates.add(cb.equal(rolesJoin.get("id"), roleIdLong)); // Compare with external Long Role ID

        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        // Add distinct to avoid duplicate groups if a group has multiple roles matching the criteria
        cq.distinct(true);
        // Optional: Add ordering


        TypedQuery<Group> query = em.createQuery(cq);

        if (firstResult != null) {
            query.setFirstResult(firstResult);
        }
        if (maxResults != null) {
            query.setMaxResults(maxResults);
        }

        return query.getResultList();
    }

    // Implemented based on CustomUserStorageProvider needs (with realm, search, exact, pagination)
    public List<Group> getTopLevelGroups(String realmId, String search, Boolean exact, Integer firstResult, Integer maxResults) {
        log.debug("GroupRepository.getTopLevelGroups(realmId={}, search={}, exact={}, first={}, max={})", realmId, search, exact, firstResult, maxResults);
        if (realmId == null || realmId.trim().isEmpty()) {
            return new ArrayList<>();
        }
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Group> cq = cb.createQuery(Group.class);
        Root<Group> root = cq.from(Group.class);
        List<Predicate> predicates = new ArrayList<>();

        // Filter by realm
        predicates.add(cb.equal(root.get("realmId"), realmId)); // Assuming Group has realmId
        // Filter for top-level groups (parentId is null)
        predicates.add(cb.isNull(root.get("parentId")));

        if (search != null && !search.trim().isEmpty()) {
            if (Boolean.TRUE.equals(exact)) {
                predicates.add(cb.equal(root.get("name"), search));
            } else {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase() + "%"));
            }
        }


        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        // Optional: Add ordering

        TypedQuery<Group> query = em.createQuery(cq);

        if (firstResult != null) {
            query.setFirstResult(firstResult);
        }
        if (maxResults != null) {
            query.setMaxResults(maxResults);
        }

        return query.getResultList();
    }


    public Group save(Group group) {
        log.debug("GroupRepository.save({})", group != null ? group.getName() : "null");
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            if (group.getId() == null) { // Assuming ID is auto-generated for new entities
                em.persist(group);
            } else { // For existing entities, check if managed before merging
                if (em.contains(group)) {
                    em.merge(group);
                } else {
                    em.merge(group);
                }
            }
            tx.commit();
            // Return the managed or merged entity which will have the ID if newly persisted
            return group;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            log.error("Failed to save group {}: {}", group != null ? group.getName() : "null", e.getMessage(), e);
            throw e;
        }
    }

    public void delete(Group group) {
        log.debug("GroupRepository.delete({})", group != null ? group.getName() : "null");
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Group merged = em.contains(group) ? group : em.merge(group);
            // Handle removal of children and associations before deleting the group itself
            // JPA cascade options (CascadeType.REMOVE) can help here, but manual logic might be needed
            // depending on your database constraints and desired behavior.
            // Example manual removal of user and role associations (requires UserRepository/RoleRepository or explicit queries):
            // TODO: Implement removeUserMappingsForGroup(String groupId) in UserRepository
            // TODO: Implement removeRoleMappingsForGroup(String groupId) in GroupRepository or here
            // userRepository.removeUserMappingsForGroup(String.valueOf(merged.getId())); // Needs implementation
            // removeRoleMappingsForGroup(String.valueOf(merged.getId())); // Needs implementation

            em.remove(merged);
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            log.error("Failed to delete group {}: {}", group != null ? group.getName() : "null", e.getMessage(), e);
            throw e;
        }
    }

    // Method to delete group by String ID, handling Long (external) or UUID (Keycloak)
    public void delete(String groupId) {
        log.debug("GroupRepository.delete(id={})", groupId);
        if (groupId == null || groupId.trim().isEmpty()) {
            log.warn("Attempted to delete group with empty ID.");
            return;
        }

        // Attempt to find the group by the provided ID (could be external Long or Keycloak UUID)
        Group group = getById(groupId); // getById handles both Long and Keycloak UUID lookups

        if (group != null) {
            // Handle removal of children and associations before deleting the group itself.
            // If not handled by cascade, do it here or in delete(Group).
            // TODO: Implement removeUserMappingsForGroup in UserRepository
            // TODO: Implement removeRoleMappingsForGroup here or elsewhere
            log.warn("delete(String groupId) - assignment/child removal not fully implemented.");
            // userRepository.removeUserMappingsForGroup(String.valueOf(group.getId())); // Needs implementation
            // removeRoleMappingsForGroup(String.valueOf(group.getId())); // Needs implementation

            delete(group); // Delegate to delete(Group)
            log.info("Successfully deleted group with ID (external or keycloak): {}", groupId);
        } else {
            log.warn("Group not found for deletion with ID (external or keycloak): {}", groupId);
        }
    }

    // Implemented based on CustomUserStorageProvider needs
    public void deleteAllGroupsByRealm(String realmId) {
        log.debug("GroupRepository.deleteAllGroupsByRealm(realmId={})", realmId);
        if (realmId == null || realmId.trim().isEmpty()) {
            log.warn("Attempted to delete all groups with empty realmId.");
            return;
        }
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            // Note: This assumes cascade deletes are configured for group_roles and subgroups.
            // If not, you need to delete from join tables and subgroups first explicitly.
            // Example explicit deletions:
            // em.createNativeQuery("DELETE FROM groups_roles gr WHERE gr.groups_id IN (SELECT g.id FROM groups g WHERE g.realmId = :realmId)")
            //    .setParameter("realmId", realmId).executeUpdate();
            // Consider deleting subgroups recursively or using a different strategy depending on your hierarchy.

            // Delete groups by realmId (relies on configured cascades or prior explicit deletes)
            em.createQuery("DELETE FROM groups g WHERE g.realmId = :realmId")
                    .setParameter("realmId", realmId)
                    .executeUpdate();
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            log.error("Failed to delete all groups for realm {}: {}", realmId, e.getMessage(), e);
            throw e;
        }
    }

    // Implemented based on CustomUserStorageProvider needs (assignment removal)
    public void removeRoleMappingsForGroup(String groupId) {
        log.debug("GroupRepository.removeRoleMappingsForGroup(groupId={})", groupId);
        if (groupId == null || groupId.trim().isEmpty()) {
            log.warn("Attempted to remove role mappings for empty groupId.");
            return;
        }

        // Resolve the Group ID (could be external Long or Keycloak UUID) to the external Long ID
        Group group = getById(groupId); // Use getById to find the group entity

        if (group == null) {
            log.warn("Group not found for removing role mappings with ID (external or keycloak): {}", groupId);
            return; // Cannot remove mappings for a non-existent group
        }

        try {
            Long groupIdLong = group.getId(); // Use the external Long ID from the entity
            EntityTransaction tx = em.getTransaction();
            tx.begin();
            // Delete entries from the groups_roles join table for the given group's external Long ID
            em.createNativeQuery("DELETE FROM groups_roles WHERE groups_id = :groupId")
                    .setParameter("groupId", groupIdLong) // Assuming groups_id column and Long ID
                    .executeUpdate();
            tx.commit();
            log.info("Successfully removed role mappings for group (external ID) {}", groupIdLong);
        } catch (Exception e) {
            log.error("Failed to remove role mappings for group with ID (external or keycloak) {}: {}", groupId, e.getMessage(), e);
            throw e; // Re-throw exception
        }
    }

    // Implemented based on CustomUserStorageProvider needs (assignment removal)
    public void deleteAllAssignmentsForRealmRoles(String realmId) {
        log.debug("GroupRepository.deleteAllAssignmentsForRealmRoles(realmId={})", realmId);
        if (realmId == null || realmId.trim().isEmpty()) {
            log.warn("Attempted to delete realm role assignments with empty realmId.");
            return;
        }
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            // Delete entries from groups_roles where the role is a realm role in the specified realm
            em.createNativeQuery("DELETE FROM groups_roles gr WHERE gr.roles_id IN (SELECT r.id FROM roles r WHERE r.realmId = :realmId AND r.clientId IS NULL)")
                    .setParameter("realmId", realmId) // Assuming Role entity has realmId
                    .executeUpdate();
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            log.error("Failed to delete all realm role assignments for realm {}: {}", realmId, e.getMessage(), e);
            throw e;
        }
    }

    // Implemented based on CustomUserStorageProvider needs (assignment removal)
    public void deleteAllAssignmentsForClientRoles(String clientId) {
        log.debug("GroupRepository.deleteAllAssignmentsForClientRoles(clientId={})", clientId);
        if (clientId == null || clientId.trim().isEmpty()) {
            log.warn("Attempted to delete client role assignments with empty clientId.");
            return;
        }
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            // Delete entries from groups_roles where the role is a client role for the specified client
            em.createNativeQuery("DELETE FROM groups_roles gr WHERE gr.roles_id IN (SELECT r.id FROM roles r WHERE r.clientId = :clientId)")
                    .setParameter("clientId", clientId) // Assuming Role entity has clientId
                    .executeUpdate();
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            log.error("Failed to delete all client role assignments for client {}: {}", clientId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Finds a group by Keycloak ID or name. If not found, creates a new one.
     * Then, ensures the group entity's properties (name, parentId, parentPath, realmId)
     * are synchronized with the provided Keycloak GroupModel.
     *
     * @param realmId The realm ID.
     * @param groupModel The Keycloak GroupModel to sync with.
     * @return The synchronized Group entity from your database.
     */
    public Group syncGroup(String realmId, GroupModel groupModel) {
        log.debug("GroupRepository.syncGroup(realmId={}, groupModel.id={}, groupModel.name={})",
                realmId, groupModel.getId(), groupModel.getName());

        Group groupEntity = null;

        // 1. Try to find by Keycloak ID first (most reliable link)
        if (groupModel.getId() != null) {
            groupEntity = findByKeycloakId(groupModel.getId());
        }

        // 2. If not found by Keycloak ID, try to find by name and parent for existing groups
        // This is important for groups that might have been created before Keycloak assigned a keycloakId,
        // or for top-level groups where Keycloak might not provide a parent ID explicitly on creation.
        if (groupEntity == null) {
            // Determine parentId for lookup. Keycloak GroupModel's parent can be null for top-level.
            String parentKeycloakId = null;
            GroupModel parentModel = groupModel.getParent();
            if (parentModel != null) {
                parentKeycloakId = parentModel.getId(); // This would be the Keycloak UUID of the parent
            }

            // Attempt to find by name and parent. This might require resolving the parent's external ID.
            Long parentExternalId = null;
            if (parentKeycloakId != null) {
                Group parentEntity = findByKeycloakId(parentKeycloakId);
                if (parentEntity != null) {
                    parentExternalId = parentEntity.getId();
                } else {
                    log.warn("Parent group with Keycloak ID {} not found in database for group {}. Cannot lookup by name and parent.",
                            parentKeycloakId, groupModel.getName());
                    // If parent doesn't exist, we can't find by name and parent. Proceed to creation.
                }
            }

            if (parentExternalId == null) { // Top-level group lookup
                groupEntity = findGroupByNameAndParent(realmId, groupModel.getName(), null);
            } else { // Subgroup lookup
                groupEntity = findGroupByNameAndParent(realmId, groupModel.getName(), String.valueOf(parentExternalId));
            }
        }


        // 3. If still not found, create a new group entity
        if (groupEntity == null) {
            log.info("Group '{}' (Keycloak ID: {}) not found in database. Creating new group.",
                    groupModel.getName(), groupModel.getId());
            groupEntity = new Group();
            groupEntity.setCreatedAt(Instant.now());
            groupEntity.setCreatedBy("keycloak-sync"); // Or a more specific user if available
        } else {
            log.info("Group '{}' (ID: {}, Keycloak ID: {}) found. Synchronizing properties.",
                    groupEntity.getName(), groupEntity.getId(), groupEntity.getKeycloakId());
        }

        // 4. Synchronize properties from Keycloak GroupModel to your Group entity
        groupEntity.setName(groupModel.getName());
        groupEntity.setRealmId(realmId); // Always ensure realmId is set

        // Set Keycloak ID if not already set or if it needs updating (should be unique)
        if (groupEntity.getKeycloakId() == null || !groupEntity.getKeycloakId().equals(groupModel.getId())) {
            // Only set if groupModel.getId() is a valid UUID, otherwise it might be a composite ID from storage
            if (isUUID(groupModel.getId())) { // Use your isUUID helper
                groupEntity.setKeycloakId(groupModel.getId());
            } else {
                // If Keycloak ID from GroupModel is a composite ID (e.g., 'f:provider_id:external_id'),
                // then the actual Keycloak ID in our 'keycloakId' column should be the raw UUID Keycloak assigns.
                // This scenario is tricky because GroupModel.getId() might be the composite ID.
                // For now, if it's not a UUID, we won't overwrite a pre-existing UUID,
                // but if it's null, we might try to infer or leave it for later.
                log.debug("GroupModel ID '{}' is not a raw UUID. Not updating keycloakId field directly.", groupModel.getId());
            }
        }


        // Handle parent relationship
        GroupModel parentKcGroup = groupModel.getParent();
        if (parentKcGroup != null) {
            // Recursively sync the parent group if it exists in Keycloak
            Group parentDbGroup = syncGroup(realmId, parentKcGroup); // Ensure parent exists and is synced
            groupEntity.setParentId(parentDbGroup.getId());
            // Parent path might also be updated if needed. Keycloak uses '/path/to/group'.
            // You can construct this from parentDbGroup.getParentPath() + "/" + parentDbGroup.getName()
            String parentPath = parentDbGroup.getParentPath();
            if (parentPath == null || parentPath.isEmpty()) {
                parentPath = "/" + parentDbGroup.getName();
            } else {
                parentPath = parentPath + "/" + parentDbGroup.getName();
            }
            groupEntity.setParentPath(parentPath);
        } else {
            // Top-level group
            groupEntity.setParentId(null);
            groupEntity.setParentPath(null); // Or "/" depending on your path convention
        }

        groupEntity.setUpdatedAt(Instant.now());
        groupEntity.setUpdatedBy("keycloak-sync"); // Or a more specific user

        // 5. Save the synchronized group entity
        return save(groupEntity); // Use your existing save method for persistence
    }
}