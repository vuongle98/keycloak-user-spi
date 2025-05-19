package org.vuong.keycloak.spi.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vuong.keycloak.spi.entity.Role;
import org.vuong.keycloak.spi.entity.UserEntity; // Keep if relationships are traversed here

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set; // Import Set
import java.util.stream.Collectors;

public class RoleRepository {

    private static final Logger log = LoggerFactory.getLogger(RoleRepository.class);
    private final EntityManager em;

    public RoleRepository(EntityManager em) {
        this.em = em;
    }

    public Role getById(String id) {
        log.debug("RoleRepository.getById({})", id);
        if (id == null || id.trim().isEmpty()) {
            return null;
        }
        try {
            // Assuming Role entity ID is Long, convert String ID to Long
            Long roleIdLong = Long.valueOf(id);
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Role> cq = cb.createQuery(Role.class);
            Root<Role> root = cq.from(Role.class);
            cq.where(cb.equal(root.get("id"), roleIdLong));
            return em.createQuery(cq).getSingleResult();
        } catch (NoResultException e) {
            log.debug("Role not found with ID: {}", id);
            return null;
        } catch (NumberFormatException e) {
            log.warn("Invalid role ID format: {}", id, e);
            return null;
        }
    }

    public Role findByName(String name) {
        log.debug("RoleRepository.findByName({})", name);
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Role> cq = cb.createQuery(Role.class);
            Root<Role> root = cq.from(Role.class);
            cq.where(cb.equal(root.get("name"), name));
            return em.createQuery(cq).getSingleResult();
        } catch (NoResultException e) {
            log.debug("Role not found with name: {}", name);
            return null;
        }
    }

    // Implemented based on CustomUserStorageProvider needs
    public Role findRealmRoleByName(String realmId, String name) {
        log.debug("RoleRepository.findRealmRoleByName(realmId={}, name={})", realmId, name);
        if (realmId == null || realmId.trim().isEmpty() || name == null || name.trim().isEmpty()) {
            return null;
        }
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Role> cq = cb.createQuery(Role.class);
            Root<Role> root = cq.from(Role.class);
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("name"), name));
            // Assuming Role entity has a realmId field
            predicates.add(cb.equal(root.get("realmId"), realmId));
            // Assuming Role entity has a clientId field and null indicates realm role
            predicates.add(cb.isNull(root.get("clientId")));
            cq.where(cb.and(predicates.toArray(new Predicate[0])));
            return em.createQuery(cq).getSingleResult();
        } catch (NoResultException e) {
            log.debug("Realm role not found with name {} in realm {}", name, realmId);
            return null;
        }
    }

    // Implemented based on CustomUserStorageProvider needs
    public Role findClientRoleByName(String realmId, String clientId, String name) {
        log.debug("RoleRepository.findClientRoleByName(realmId={}, clientId={}, name={})", realmId, clientId, name);
        if (realmId == null || realmId.trim().isEmpty() || clientId == null || clientId.trim().isEmpty() || name == null || name.trim().isEmpty()) {
            return null;
        }
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Role> cq = cb.createQuery(Role.class);
            Root<Role> root = cq.from(Role.class);
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("name"), name));
            // Assuming Role entity has realmId and clientId fields
            predicates.add(cb.equal(root.get("realmId"), realmId));
            predicates.add(cb.equal(root.get("clientId"), clientId));
            cq.where(cb.and(predicates.toArray(new Predicate[0])));
            return em.createQuery(cq).getSingleResult();
        } catch (NoResultException e) {
            log.debug("Client role not found with name {} for client {} in realm {}", name, clientId, realmId);
            return null;
        }
    }


    public List<Role> search(String keyword) {
        log.debug("RoleRepository.search({}) - generic search (not realm/client filtered)", keyword);
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Role> cq = cb.createQuery(Role.class);
        Root<Role> root = cq.from(Role.class);

        if (keyword != null && !keyword.isEmpty()) {
            cq.where(cb.like(cb.lower(root.get("name")), "%" + keyword.toLowerCase() + "%"));
        }

        return em.createQuery(cq).getResultList();
    }

    // Implemented based on CustomUserStorageProvider needs
    public List<Role> searchRealmRoles(String realmId, String search, Integer first, Integer max) {
        log.debug("RoleRepository.searchRealmRoles(realmId={}, search={}, first={}, max={})", realmId, search, first, max);
        if (realmId == null || realmId.trim().isEmpty()) {
            return new ArrayList<>();
        }
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Role> cq = cb.createQuery(Role.class);
        Root<Role> root = cq.from(Role.class);
        List<Predicate> predicates = new ArrayList<>();

        // Filter by realm
        predicates.add(cb.equal(root.get("realmId"), realmId));
        // Filter for realm roles (clientId is null)
        predicates.add(cb.isNull(root.get("clientId")));

        if (search != null && !search.isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase() + "%"));
        }

        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        TypedQuery<Role> query = em.createQuery(cq);

        if (first != null) {
            query.setFirstResult(first);
        }
        if (max != null) {
            query.setMaxResults(max);
        }

        return query.getResultList();
    }

    // Implemented based on CustomUserStorageProvider needs
    public List<Role> searchClientRoles(String realmId, String clientId, String search, Integer first, Integer max) {
        log.debug("RoleRepository.searchClientRoles(realmId={}, clientId={}, search={}, first={}, max={})", realmId, clientId, search, first, max);
        if (realmId == null || realmId.trim().isEmpty() || clientId == null || clientId.trim().isEmpty()) {
            return new ArrayList<>();
        }
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Role> cq = cb.createQuery(Role.class);
        Root<Role> root = cq.from(Role.class);
        List<Predicate> predicates = new ArrayList<>();

        // Filter by realm and client
        predicates.add(cb.equal(root.get("realmId"), realmId));
        predicates.add(cb.equal(root.get("clientId"), clientId));

        if (search != null && !search.isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase() + "%"));
        }

        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        TypedQuery<Role> query = em.createQuery(cq);

        if (first != null) {
            query.setFirstResult(first);
        }
        if (max != null) {
            query.setMaxResults(max);
        }

        return query.getResultList();
    }

    // Implemented based on CustomUserStorageProvider needs
    public List<Role> searchClientRolesByClientIds(String realmId, List<String> clientIds, String search, Integer first, Integer max) {
        log.debug("RoleRepository.searchClientRolesByClientIds(realmId={}, clientIds={}, search={}, first={}, max={})", realmId, clientIds, search, first, max);
        if (realmId == null || realmId.trim().isEmpty() || clientIds == null || clientIds.isEmpty()) {
            return new ArrayList<>();
        }
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Role> cq = cb.createQuery(Role.class);
        Root<Role> root = cq.from(Role.class);
        List<Predicate> predicates = new ArrayList<>();

        // Filter by realm
        predicates.add(cb.equal(root.get("realmId"), realmId));
        // Filter by list of client IDs
        predicates.add(root.get("clientId").in(clientIds));

        if (search != null && !search.isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase() + "%"));
        }

        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        TypedQuery<Role> query = em.createQuery(cq);

        if (first != null) {
            query.setFirstResult(first);
        }
        if (max != null) {
            query.setMaxResults(max);
        }

        return query.getResultList();
    }

    // Implemented based on CustomUserStorageProvider needs
    public List<Role> searchClientRolesExcludingClientIds(String realmId, List<String> excludedClientIds, String search, Integer first, Integer max) {
        log.debug("RoleRepository.searchClientRolesExcludingClientIds(realmId={}, excludedClientIds={}, search={}, first={}, max={})", realmId, excludedClientIds, search, first, max);
        if (realmId == null || realmId.trim().isEmpty()) {
            return new ArrayList<>();
        }
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Role> cq = cb.createQuery(Role.class);
        Root<Role> root = cq.from(Role.class);
        List<Predicate> predicates = new ArrayList<>();

        // Filter by realm
        predicates.add(cb.equal(root.get("realmId"), realmId));
        // Ensure it's a client role (clientId is not null)
        predicates.add(cb.isNotNull(root.get("clientId")));
        // Exclude list of client IDs
        if (excludedClientIds != null && !excludedClientIds.isEmpty()) {
            predicates.add(cb.not(root.get("clientId").in(excludedClientIds)));
        }


        if (search != null && !search.isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase() + "%"));
        }

        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        TypedQuery<Role> query = em.createQuery(cq);

        if (first != null) {
            query.setFirstResult(first);
        }
        if (max != null) {
            query.setMaxResults(max);
        }

        return query.getResultList();
    }


    public List<Role> getAllRoles() {
        log.debug("RoleRepository.getAllRoles() - generic get all (not realm/client filtered)");
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Role> cq = cb.createQuery(Role.class);
        cq.from(Role.class);
        return em.createQuery(cq).getResultList();
    }

    // Implemented based on CustomUserStorageProvider needs
    public List<Role> getRealmRoles(String realmId, Integer first, Integer max) {
        log.debug("RoleRepository.getRealmRoles(realmId={}, first={}, max={})", realmId, first, max);
        if (realmId == null || realmId.trim().isEmpty()) {
            return new ArrayList<>();
        }
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Role> cq = cb.createQuery(Role.class);
        Root<Role> root = cq.from(Role.class);
        List<Predicate> predicates = new ArrayList<>();

        // Filter by realm and for realm roles (clientId is null)
        predicates.add(cb.equal(root.get("realmId"), realmId));
        predicates.add(cb.isNull(root.get("clientId")));

        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        TypedQuery<Role> query = em.createQuery(cq);

        if (first != null) {
            query.setFirstResult(first);
        }
        if (max != null) {
            query.setMaxResults(max);
        }

        return query.getResultList();
    }

    // Implemented based on CustomUserStorageProvider needs
    public List<Role> getClientRoles(String realmId, String clientId, Integer first, Integer max) {
        log.debug("RoleRepository.getClientRoles(realmId={}, clientId={}, first={}, max={})", realmId, clientId, first, max);
        if (realmId == null || realmId.trim().isEmpty() || clientId == null || clientId.trim().isEmpty()) {
            return new ArrayList<>();
        }
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Role> cq = cb.createQuery(Role.class);
        Root<Role> root = cq.from(Role.class);
        List<Predicate> predicates = new ArrayList<>();

        // Filter by realm and client
        predicates.add(cb.equal(root.get("realmId"), realmId));
        predicates.add(cb.equal(root.get("clientId"), clientId));

        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        TypedQuery<Role> query = em.createQuery(cq);

        if (first != null) {
            query.setFirstResult(first);
        }
        if (max != null) {
            query.setMaxResults(max);
        }

        return query.getResultList();
    }

    // Implemented based on CustomUserStorageProvider needs
    public List<Role> getRolesByIdsOrSearch(String realmId, List<String> roleIds, String search, Integer first, Integer max) {
        log.debug("RoleRepository.getRolesByIdsOrSearch(realmId={}, roleIds={}, search={}, first={}, max={})", realmId, roleIds, search, first, max);
        if (realmId == null || realmId.trim().isEmpty()) {
            return new ArrayList<>();
        }
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Role> cq = cb.createQuery(Role.class);
        Root<Role> root = cq.from(Role.class);
        List<Predicate> predicates = new ArrayList<>();

        // Filter by realm
        predicates.add(cb.equal(root.get("realmId"), realmId));

        List<Long> roleIdsLong = new ArrayList<>();
        if (roleIds != null && !roleIds.isEmpty()) {
            roleIdsLong = roleIds.stream()
                    .map(id -> {
                        try { return Long.valueOf(id); }
                        catch (NumberFormatException e) { log.warn("Invalid role ID format in list: {}", id); return null; }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }


        if (search != null && !search.trim().isEmpty()) {
            Predicate searchPredicate = cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase() + "%");
            if (!roleIdsLong.isEmpty()) {
                // If both IDs and search are provided, typically it's IDs OR search criteria matching
                predicates.add(cb.or(root.get("id").in(roleIdsLong), searchPredicate));
            } else {
                // Only search criteria
                predicates.add(searchPredicate);
            }
        } else if (!roleIdsLong.isEmpty()) {
            // Only IDs provided
            predicates.add(root.get("id").in(roleIdsLong));
        } else {
            // No IDs and no search term, return empty list as per Keycloak behavior expectation
            return new ArrayList<>();
        }

        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        TypedQuery<Role> query = em.createQuery(cq);

        if (first != null) {
            query.setFirstResult(first);
        }
        if (max != null) {
            query.setMaxResults(max);
        }

        return query.getResultList();
    }


    public void save(Role role) {
        log.debug("RoleRepository.save({})", role != null ? role.getName() : "null");
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            if (role.getId() == null) { // Assuming ID is auto-generated for new entities
                em.persist(role);
            } else { // For existing entities, check if managed before merging
                if (em.contains(role)) {
                    em.merge(role);
                } else {
                    em.merge(role);
                }
            }
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            log.error("Failed to save role {}: {}", role != null ? role.getName() : "null", e.getMessage(), e);
            throw e;
        }
    }

    public void delete(Role role) {
        log.debug("RoleRepository.delete({})", role != null ? role.getName() : "null");
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Role merged = em.contains(role) ? role : em.merge(role);
            em.remove(merged);
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            log.error("Failed to delete role {}: {}", role != null ? role.getName() : "null", e.getMessage(), e);
            throw e;
        }
    }

    // Implemented based on CustomUserStorageProvider needs
    public void deleteAllRealmRoles(String realmId) {
        log.debug("RoleRepository.deleteAllRealmRoles(realmId={})", realmId);
        if (realmId == null || realmId.trim().isEmpty()) {
            log.warn("Attempted to delete all realm roles with empty realmId.");
            return;
        }
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            // Delete realm roles by realmId and clientId is null
            em.createQuery("DELETE FROM roles r WHERE r.realmId = :realmId AND r.clientId IS NULL")
                    .setParameter("realmId", realmId)
                    .executeUpdate();
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            log.error("Failed to delete all realm roles for realm {}: {}", realmId, e.getMessage(), e);
            throw e;
        }
    }

    // Implemented based on CustomUserStorageProvider needs
    public void deleteAllClientRoles(String realmId, String clientId) {
        log.debug("RoleRepository.deleteAllClientRoles(realmId={}, clientId={})", realmId, clientId);
        if (realmId == null || realmId.trim().isEmpty() || clientId == null || clientId.trim().isEmpty()) {
            log.warn("Attempted to delete all client roles with empty realmId or clientId.");
            return;
        }
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            // Delete client roles by realmId and clientId
            em.createQuery("DELETE FROM roles r WHERE r.realmId = :realmId AND r.clientId = :clientId")
                    .setParameter("realmId", realmId)
                    .setParameter("clientId", clientId)
                    .executeUpdate();
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            log.error("Failed to delete all client roles for client {} in realm {}: {}", clientId, realmId, e.getMessage(), e);
            throw e;
        }
    }

    // Methods for managing role assignments on users/groups would typically be here or in dedicated repositories.
    // Example placeholder signatures (implementation would involve join tables users_roles, groups_roles):
    // public void removeUserMappingsForRole(String roleId) { ... }
    // public void removeGroupMappingsForRole(String roleId) { ... }
    // public void deleteAllAssignmentsForRealmRoles(String realmId) { ... }
    // public void deleteAllAssignmentsForClientRoles(String clientId) { ... }
}