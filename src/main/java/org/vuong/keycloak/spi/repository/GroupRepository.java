package org.vuong.keycloak.spi.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vuong.keycloak.spi.entity.Group;
import org.vuong.keycloak.spi.entity.Role; // Needed for join

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;


public class GroupRepository {

    private static final Logger log = LoggerFactory.getLogger(GroupRepository.class);
    private final EntityManager em;

    public GroupRepository(EntityManager em) {
        this.em = em;
    }

    public Group getById(String id) {
        log.debug("GroupRepository.getById({})", id);
        if (id == null || id.trim().isEmpty()) {
            return null;
        }
        try {
            // Assuming Group entity ID is Long, convert String ID to Long
            Long groupIdLong = Long.valueOf(id);
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Group> cq = cb.createQuery(Group.class);
            Root<Group> root = cq.from(Group.class);
            cq.where(cb.equal(root.get("id"), groupIdLong));
            return em.createQuery(cq).getSingleResult();
        } catch (NoResultException e) {
            log.debug("Group not found with ID: {}", id);
            return null;
        } catch (NumberFormatException e) {
            log.warn("Invalid group ID format: {}", id, e);
            return null;
        }
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
                Long parentIdLong = Long.valueOf(parentId);
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
        if (groupIds != null && !groupIds.isEmpty()) {
            groupIdsLong = groupIds.stream()
                    .map(id -> {
                        try { return Long.valueOf(id); }
                        catch (NumberFormatException e) { log.warn("Invalid group ID format in list: {}", id); return null; }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        if (search != null && !search.trim().isEmpty()) {
            Predicate searchPredicate = cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase() + "%");
            if (!groupIdsLong.isEmpty()) {
                // If both IDs and search are provided, typically it's IDs OR search criteria matching
                predicates.add(cb.or(root.get("id").in(groupIdsLong), searchPredicate));
            } else {
                // Only search criteria
                predicates.add(searchPredicate);
            }
        } else if (!groupIdsLong.isEmpty()) {
            // Only IDs provided
            predicates.add(root.get("id").in(groupIdsLong));
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
        Long roleIdLong;
        try {
            roleIdLong = Long.valueOf(roleId);
        } catch (NumberFormatException e) {
            log.warn("Invalid role ID format for findGroupsByRoleId: {}", roleId, e);
            return new ArrayList<>(); // Invalid role ID format
        }

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Group> cq = cb.createQuery(Group.class);
        Root<Group> root = cq.from(Group.class);
        Join<Group, Role> rolesJoin = root.join("roles"); // Join the many-to-many roles relationship

        List<Predicate> predicates = new ArrayList<>();

        // Filter by realm (assuming Group has realmId)
        predicates.add(cb.equal(root.get("realmId"), realmId)); // Assuming Group has realmId
        // Filter by role ID in the joined table
        predicates.add(cb.equal(rolesJoin.get("id"), roleIdLong));

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

    // Method to delete group by String ID, converting to Long.
    // Useful if CustomUserStorageProvider prefers passing String IDs directly for deletion.
    public void delete(String groupId) {
        log.debug("GroupRepository.delete(id={})", groupId);
        if (groupId == null || groupId.trim().isEmpty()) {
            log.warn("Attempted to delete group with empty ID.");
            return;
        }
        try {
            Long groupIdLong = Long.valueOf(groupId);
            Group group = em.find(Group.class, groupIdLong);
            if (group != null) {
                // Handle removal of children and associations before deleting the group itself.
                // If not handled by cascade, do it here or in delete(Group).
                // TODO: Implement removeUserMappingsForGroup in UserRepository
                // TODO: Implement removeRoleMappingsForGroup here or elsewhere
                log.warn("delete(String groupId) - assignment/child removal not fully implemented.");
                // userRepository.removeUserMappingsForGroup(String.valueOf(group.getId()));
                // removeRoleMappingsForGroup(String.valueOf(group.getId()));

                delete(group); // Delegate to delete(Group)
            } else {
                log.warn("Group not found for deletion with ID: {}", groupId);
            }
        } catch (NumberFormatException e) {
            log.warn("Invalid group ID format for deletion: {}", groupId, e);
        } catch (Exception e) {
            log.error("Failed to delete group with ID {}: {}", groupId, e.getMessage(), e);
            throw e; // Re-throw exception after logging
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
        try {
            Long groupIdLong = Long.valueOf(groupId);
            EntityTransaction tx = em.getTransaction();
            tx.begin();
            // Delete entries from the groups_roles join table for the given group ID
            em.createNativeQuery("DELETE FROM groups_roles WHERE groups_id = :groupId")
                    .setParameter("groupId", groupIdLong) // Assuming groups_id column and Long ID
                    .executeUpdate();
            tx.commit();
        } catch (NumberFormatException e) {
            log.warn("Invalid group ID format for removeRoleMappingsForGroup: {}", groupId, e);
        } catch (Exception e) {
            log.error("Failed to remove role mappings for group {}: {}", groupId, e.getMessage(), e);
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
}