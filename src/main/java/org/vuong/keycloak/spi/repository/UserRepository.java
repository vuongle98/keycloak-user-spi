package org.vuong.keycloak.spi.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vuong.keycloak.spi.entity.Group;
import org.vuong.keycloak.spi.entity.Role;
import org.vuong.keycloak.spi.entity.UserEntity;
import org.vuong.keycloak.spi.entity.UserProfile;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class UserRepository {

    private static final Logger log = LoggerFactory.getLogger(UserRepository.class);
    private final EntityManager em;

    public UserRepository(EntityManager em) {
        this.em = em;
    }

    public UserEntity findByUsername(String username) {
        log.debug("UserRepository.findByUsername({})", username);
        if (username == null || username.trim().isEmpty()) {
            return null;
        }
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<UserEntity> cq = cb.createQuery(UserEntity.class);
            Root<UserEntity> root = cq.from(UserEntity.class);
            // Eagerly fetch the 'groups' and 'roles' collections to avoid LazyInitializationException
            root.fetch("groups", JoinType.LEFT);
            root.fetch("roles", JoinType.LEFT);
            cq.select(root).distinct(true); // Use distinct to avoid duplicate results if a user has multiple groups/roles
            cq.where(cb.equal(root.get("username"), username));
            return em.createQuery(cq).getSingleResult();
        } catch (NoResultException e) {
            log.debug("User not found with username: {}", username);
            return null;
        }
    }

    // TODO: Add findByUsernameAndRealm(String realmId, String username) if usernames are unique per realm

    public UserEntity findByEmail(String email) {
        log.debug("UserRepository.findByEmail({})", email);
        if (email == null || email.trim().isEmpty()) {
            return null;
        }
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<UserEntity> cq = cb.createQuery(UserEntity.class);
            Root<UserEntity> root = cq.from(UserEntity.class);
            // Eagerly fetch the 'groups' and 'roles' collections to avoid LazyInitializationException
            root.fetch("groups", JoinType.LEFT);
            root.fetch("roles", JoinType.LEFT);
            cq.select(root).distinct(true);
            cq.where(cb.equal(root.get("email"), email));
            return em.createQuery(cq).getSingleResult();
        } catch (NoResultException e) {
            log.debug("User not found with email: {}", email);
            return null;
        }
    }

    // Add this new method to find a user by their Keycloak UUID
    public UserEntity findByKeycloakId(String keycloakId) {
        log.debug("UserRepository.findByKeycloakId({})", keycloakId);
        if (keycloakId == null || keycloakId.trim().isEmpty()) {
            return null;
        }
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<UserEntity> cq = cb.createQuery(UserEntity.class);
            Root<UserEntity> root = cq.from(UserEntity.class);
            // Eagerly fetch the 'groups' and 'roles' collections to avoid LazyInitializationException
            root.fetch("groups", JoinType.LEFT);
            root.fetch("roles", JoinType.LEFT);
            cq.select(root).distinct(true);
            cq.where(cb.equal(root.get("keycloakId"), keycloakId));
            return em.createQuery(cq).getSingleResult();
        } catch (NoResultException e) {
            log.debug("User not found with keycloakId: {}", keycloakId);
            return null;
        }
    }


    // Keep the existing search method, note it doesn't filter by realm
    public List<UserEntity> search(String search, String username, String email, Integer firstResult, Integer maxResults) {
        log.debug("UserRepository.search(search={}, username={}, email={}, first={}, max={}) - (not realm filtered)", search, username, email, firstResult, maxResults);
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<UserEntity> cq = cb.createQuery(UserEntity.class);
        Root<UserEntity> root = cq.from(UserEntity.class);

        List<Predicate> predicates = new ArrayList<>();

        if (username != null && !username.isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("username")), "%" + username.toLowerCase() + "%"));
        }

        if (email != null && !email.isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase() + "%"));
        }

        if (search != null && !search.isEmpty()) {
            // Combine search across username and email
            predicates.add(cb.or(
                    cb.like(cb.lower(root.get("username")), "%" + search.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("email")), "%" + search.toLowerCase() + "%")
                    // TODO: Add search across UserProfile fields (firstName, lastName, etc.) here if needed
            ));
        }
        // TODO: Add realm filtering here if necessary: predicates.add(cb.equal(root.get("realmId"), realmId));


        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        // Optional: Add ordering, e.g., cq.orderBy(cb.asc(root.get("username")));

        TypedQuery<UserEntity> query = em.createQuery(cq);

        if (firstResult != null) {
            query.setFirstResult(firstResult);
        }
        if (maxResults != null) {
            query.setMaxResults(maxResults);
        }

        return query.getResultList();
    }

    public List<UserEntity> search(String realmId, String search, String username, String email,
                                   String firstName, String lastName, Boolean exact, Boolean emailVerified,
                                   Boolean enabled, String idpAlias, String idpUserId,
                                   Integer firstResult, Integer maxResults) {
        log.debug("UserRepository.search(...)");
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<UserEntity> cq = cb.createQuery(UserEntity.class);
        Root<UserEntity> root = cq.from(UserEntity.class);
        List<Predicate> predicates = new ArrayList<>();

        predicates.add(cb.equal(root.get("realmId"), realmId));

        if (search != null && !search.isEmpty()) {
            Predicate searchPredicate = cb.or(
                    buildLikePredicate(cb, root.get("username"), search, exact),
                    buildLikePredicate(cb, root.get("email"), search, exact),
                    buildLikePredicate(cb, root.get("firstName"), search, exact),
                    buildLikePredicate(cb, root.get("lastName"), search, exact)
            );
            predicates.add(searchPredicate);
        } else {
            if (username != null && !username.isEmpty()) {
                predicates.add(buildLikePredicate(cb, root.get("username"), username, exact));
            }
            if (email != null && !email.isEmpty()) {
                predicates.add(buildLikePredicate(cb, root.get("email"), email, exact));
            }
            if (firstName != null && !firstName.isEmpty()) {
                predicates.add(buildLikePredicate(cb, root.get("firstName"), firstName, exact));
            }
            if (lastName != null && !lastName.isEmpty()) {
                predicates.add(buildLikePredicate(cb, root.get("lastName"), lastName, exact));
            }
        }

        if (emailVerified != null) {
            predicates.add(cb.equal(root.get("emailVerified"), emailVerified));
        }
        if (enabled != null) {
            predicates.add(cb.equal(root.get("enabled"), enabled));
        }
        if (idpAlias != null && !idpAlias.isEmpty()) {
            // Assuming you have a way to join with federated identities
            // This is a simplified example and might need adjustments based on your entity model
            // predicates.add(cb.equal(root.join("federatedIdentities").get("identityProviderId"), idpAlias));
            log.warn("IDP Alias search not fully implemented in this example.");
        }
        if (idpUserId != null && !idpUserId.isEmpty()) {
            // Assuming you have a way to join with federated identities
            // This is a simplified example and might need adjustments based on your entity model
            // predicates.add(cb.equal(root.join("federatedIdentities").get("userId"), idpUserId));
            log.warn("IDP User ID search not fully implemented in this example.");
        }

        cq.where(cb.and(predicates.toArray(new Predicate[0])));

        TypedQuery<UserEntity> query = em.createQuery(cq);
        if (firstResult != null) {
            query.setFirstResult(firstResult);
        }
        if (maxResults != null) {
            query.setMaxResults(maxResults);
        }
        return query.getResultList();
    }

    private Predicate buildLikePredicate(CriteriaBuilder cb, jakarta.persistence.criteria.Expression<String> field, String value, Boolean exact) {
        if (exact != null && exact) {
            return cb.equal(field, value);
        } else {
            return cb.like(cb.lower(field), "%" + value.toLowerCase() + "%");
        }
    }


    public UserEntity getById(String id) {
        log.debug("UserRepository.getById({})", id);
        if (id == null || id.trim().isEmpty()) {
            return null;
        }
        try {
            // Assuming UserEntity ID is Long, attempt to convert String ID to Long
            Long userIdLong = Long.valueOf(id);
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<UserEntity> cq = cb.createQuery(UserEntity.class);
            Root<UserEntity> root = cq.from(UserEntity.class);
            // Eagerly fetch the 'groups' and 'roles' collections to avoid LazyInitializationException
            root.fetch("groups", JoinType.LEFT);
            root.fetch("roles", JoinType.LEFT);
            cq.select(root).distinct(true);
            cq.where(cb.equal(root.get("id"), userIdLong));
            return em.createQuery(cq).getSingleResult();
        } catch (NoResultException e) {
            log.debug("User not found with Long ID: {}", id);
            return null;
        } catch (NumberFormatException e) {
            // The provided ID is not a valid Long. This is expected if Keycloak passes a UUID.
            log.debug("User ID {} is not a valid Long format.", id);
            // Return null. The provider will then attempt findByKeycloakId.
            return null;
        }
    }

    public int countUsers() {
        log.debug("UserRepository.countUsers() - (not realm filtered)");
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Long> cq = cb.createQuery(Long.class);
            Root<UserEntity> root = cq.from(UserEntity.class);
            cq.select(cb.count(root));
            // TODO: Add realm filtering here if necessary: cq.where(cb.equal(root.get("realmId"), realmId));
            return em.createQuery(cq).getSingleResult().intValue();
        } catch (NoResultException e) {
            return 0;
        }
    }

    // Implemented based on CustomUserStorageProvider needs
    public int countUsersByRealm(String realmId) {
        log.debug("UserRepository.countUsersByRealm(realmId={})", realmId);
        if (realmId == null || realmId.trim().isEmpty()) {
            return 0;
        }
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Long> cq = cb.createQuery(Long.class);
            Root<UserEntity> root = cq.from(UserEntity.class);
            List<Predicate> predicates = new ArrayList<>();

            // Filter by realm
            predicates.add(cb.equal(root.get("realmId"), realmId)); // Assuming UserEntity has realmId

            cq.select(cb.count(root)).where(cb.and(predicates.toArray(new Predicate[0])));
            return em.createQuery(cq).getSingleResult().intValue();
        } catch (NoResultException e) {
            return 0;
        }
    }


    // Updated save function to return UserEntity
    public UserEntity save(UserEntity user) {
        log.debug("UserRepository.save({})", user != null ? user.getUsername() : "null");
        EntityTransaction tx = em.getTransaction();
        UserEntity savedUser = null; // Variable to hold the saved/merged entity
        try {
            tx.begin();
            if (user.getId() == null) {
                em.persist(user);
                // After persist and before commit, the user object is managed and ID is usually generated
                savedUser = user;
            } else {
                savedUser = em.merge(user); // merge returns the managed instance
            }
            tx.commit();
            log.debug("Successfully saved user {}", savedUser != null ? savedUser.getUsername() : "null");
            return savedUser; // Return the managed/persisted entity
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            log.error("Failed to save user {}: {}", user != null ? user.getUsername() : "null", e.getMessage(), e);
            throw e; // Re-throw the exception
        }
    }

    public void delete(UserEntity user) {
        log.debug("UserRepository.delete({})", user != null ? user.getUsername() : "null");
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            UserEntity merged = em.contains(user) ? user : em.merge(user);
            em.remove(merged);
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            log.error("Failed to delete user {}: {}", user != null ? user.getUsername() : "null", e.getMessage(), e);
            throw e;
        }
    }

    public void delete(String userId) {
        log.debug("UserRepository.delete(id={})", userId);
        if (userId == null || userId.trim().isEmpty()) {
            log.warn("Attempted to delete user with empty ID.");
            return;
        }
        // Attempt to find by Long ID first
        UserEntity user = getById(userId); // getById handles NumberFormatException

        // If not found by Long ID, attempt to find by Keycloak ID (UUID)
        if (user == null) {
            user = findByKeycloakId(userId);
        }

        if (user != null) {
            // Note: Ensure cascading deletes are configured for UserProfile, user_roles, user_groups
            // or handle them explicitly here or in delete(UserEntity) before removing the user entity.
            delete(user); // Delegate to delete(UserEntity)
            log.info("Successfully deleted user: {}", userId);
        } else {
            log.warn("User not found for deletion with ID (Long or Keycloak): {}", userId);
        }
    }

    /**
     * Deletes a user by their Keycloak ID.
     * @param keycloakId The Keycloak UUID of the user to delete.
     */
    public void deleteByKeycloakId(String keycloakId) {
        log.debug("UserRepository.deleteByKeycloakId({})", keycloakId);
        if (keycloakId == null || keycloakId.trim().isEmpty()) {
            log.warn("Attempted to delete user with empty Keycloak ID.");
            return;
        }
        UserEntity user = findByKeycloakId(keycloakId);
        if (user != null) {
            delete(user);
            log.info("Successfully deleted user with Keycloak ID: {}", keycloakId);
        } else {
            log.warn("User not found for deletion with Keycloak ID: {}", keycloakId);
        }
    }


    public List<UserEntity> findUsersByGroupId(String realmId, String externalGroupId, Integer firstResult, Integer maxResults) {
        log.debug("findUsersByGroupId: realmId={}, externalGroupId={}, firstResult={}, maxResults={}", realmId, externalGroupId, firstResult, maxResults);
        if (realmId == null || externalGroupId == null || externalGroupId.trim().isEmpty()) {
            log.warn("findUsersByGroupId: Invalid parameters received: realmId={}, externalGroupId={}", realmId, externalGroupId);
            return new ArrayList<>();
        }

        try {
            Long groupIdLong = Long.valueOf(externalGroupId); // Ensure it's a Long

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<UserEntity> cq = cb.createQuery(UserEntity.class);
            Root<UserEntity> root = cq.from(UserEntity.class);

            // Use INNER JOIN to ensure we only get users actually linked to this group
            Join<UserEntity, Group> groupsJoin = root.join("groups", JoinType.INNER);

            // Eagerly fetch the 'roles' collection for the returned UserEntities
            root.fetch("groups", JoinType.LEFT);
            root.fetch("roles", JoinType.LEFT);

            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(groupsJoin.get("realmId"), realmId)); // Filter by realm
            predicates.add(cb.equal(groupsJoin.get("id"), groupIdLong)); // Filter by the external group ID via the join

            cq.where(cb.and(predicates.toArray(new Predicate[0])));
            cq.select(root).distinct(true); // Select the root entity and ensure distinct results

            TypedQuery<UserEntity> query = em.createQuery(cq);

            // Log the query parameters before execution
            log.debug("findUsersByGroupId: Executing query with parameters - realmId: {}, groupId: {}, firstResult: {}, maxResults: {}",
                    realmId, groupIdLong, firstResult, maxResults);

            if (firstResult != null) {
                query.setFirstResult(firstResult);
            }
            if (maxResults != null) {
                query.setMaxResults(maxResults);
            }
            List<UserEntity> resultList = query.getResultList();
            log.debug("findUsersByGroupId: Query returned {} users for group ID {} in realm {}.", resultList.size(), externalGroupId, realmId);
            return resultList;
        } catch (NumberFormatException e) {
            log.error("findUsersByGroupId: Invalid externalGroupId format provided: {}", externalGroupId, e);
            return new ArrayList<>();
        } catch (NoResultException e) {
            log.debug("findUsersByGroupId: No users found for group ID {} in realm {}", externalGroupId, realmId);
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("findUsersByGroupId: Error fetching users by group ID {}: {}", externalGroupId, e.getMessage(), e);
            throw e; // Re-throw to make the issue visible higher up
        }
    }

    public List<UserEntity> findUsersByAttribute(String realmId, String attrName, String attrValue) {
        log.debug("UserRepository.findUsersByAttribute(realmId={}, attrName={}, attrValue={})", realmId, attrName, attrValue);
        if (realmId == null || realmId.trim().isEmpty() || attrName == null || attrName.trim().isEmpty() || attrValue == null || attrValue.trim().isEmpty()) {
            return new ArrayList<>();
        }

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<UserEntity> cq = cb.createQuery(UserEntity.class);
        Root<UserEntity> root = cq.from(UserEntity.class);
        List<Predicate> predicates = new ArrayList<>();

        // Filter by realm (assuming UserEntity has realmId)
        predicates.add(cb.equal(root.get("realmId"), realmId));

        // --- Handle common attributes based on UserEntity and UserProfile ---
        // This is a simplified example. You might need a more complex structure
        // if you have truly dynamic user attributes.
        switch (attrName) {
            case "username":
                predicates.add(cb.like(cb.lower(root.get("username")), "%" + attrValue.toLowerCase() + "%"));
                break;
            case "email":
                predicates.add(cb.like(cb.lower(root.get("email")), "%" + attrValue.toLowerCase() + "%"));
                break;
            case "locked":
                // Assuming attrValue is "true" or "false"
                try {
                    boolean lockedStatus = Boolean.parseBoolean(attrValue);
                    predicates.add(cb.equal(root.get("locked"), lockedStatus));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid boolean value for 'locked' attribute search: {}", attrValue);
                    return new ArrayList<>(); // Invalid value, no results
                }
                break;
            case "isVerifiedEmail":
                // Assuming attrValue is "true" or "false"
                try {
                    boolean verifiedStatus = Boolean.parseBoolean(attrValue);
                    predicates.add(cb.equal(root.get("isVerifiedEmail"), verifiedStatus));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid boolean value for 'isVerifiedEmail' attribute search: {}", attrValue);
                    return new ArrayList<>(); // Invalid value, no results
                }
                break;
            // --- Handle UserProfile attributes ---
            case "firstName":
            case "lastName":
            case "phone":
            case "address":
            case "avatarUrl":
                Join<UserEntity, UserProfile> profileJoin = root.join("profile"); // Join the one-to-one profile relationship
                predicates.add(cb.like(cb.lower(profileJoin.get(attrName)), "%" + attrValue.toLowerCase() + "%"));
                break;
            // Add other attributes as needed
            default:
                log.warn("Unsupported attribute name for search: {}", attrName);
                return new ArrayList<>(); // Attribute not supported for search
        }

        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        // Optional: Add ordering

        return em.createQuery(cq).getResultList();
    }


    public void deleteAllUsersByRealm(String realmId) {
        log.debug("UserRepository.deleteAllUsersByRealm(realmId={})", realmId);
        if (realmId == null || realmId.trim().isEmpty()) {
            log.warn("Attempted to delete all users with empty realmId.");
            return;
        }
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            // Note: This assumes cascade deletes are configured for UserProfile, user_roles, user_groups.
            // If not, you need to delete from join tables and UserProfile first explicitly.
            // Example explicit deletions (assuming join table names and column names):
            // em.createNativeQuery("DELETE FROM users_roles ur WHERE ur.users_id IN (SELECT u.id FROM users u WHERE u.realmId = :realmId)")
            //    .setParameter("realmId", realmId).executeUpdate();
            // em.createNativeQuery("DELETE FROM users_groups ug WHERE ug.user_id IN (SELECT u.id FROM users u WHERE u.realmId = :realmId)")
            //    .setParameter("realmId", realmId).executeUpdate();
            // em.createQuery("DELETE FROM user_profiles up WHERE up.user.realmId = :realmId") // Corrected path for UserProfile
            //    .setParameter("realmId", realmId).executeUpdate();

            // Delete users by realmId (relies on configured cascades or prior explicit deletes)
            em.createQuery("DELETE FROM users u WHERE u.realmId = :realmId")
                    .setParameter("realmId", realmId)
                    .executeUpdate();
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            log.error("Failed to delete all users for realm {}: {}", realmId, e.getMessage(), e);
            throw e;
        }
    }

    // Implemented based on CustomUserStorageProvider needs (assignment removal)
    public void removeUserMappingsForGroup(String groupId) {
        log.debug("UserRepository.removeUserMappingsForGroup(groupId={})", groupId);
        if (groupId == null || groupId.trim().isEmpty()) {
            log.warn("Attempted to remove user mappings for empty groupId.");
            return;
        }
        try {
            Long groupIdLong = Long.valueOf(groupId);
            EntityTransaction tx = em.getTransaction();
            tx.begin();
            // Delete entries from the users_groups join table for the given group ID
            em.createNativeQuery("DELETE FROM users_groups WHERE groups_id = :groupId")
                    .setParameter("groupId", groupIdLong) // Assuming groups_id column and Long ID
                    .executeUpdate();
            tx.commit();
        } catch (NumberFormatException e) {
            log.warn("Invalid group ID format for removeUserMappingsForGroup: {}", groupId, e);
        } catch (Exception e) {
            log.error("Failed to remove user mappings for group {}: {}", groupId, e.getMessage(), e);
            throw e; // Re-throw exception
        }
    }

    // Implemented based on CustomUserStorageProvider needs (assignment removal)
    public void removeUserMappingsForRole(String roleId) {
        log.debug("UserRepository.removeUserMappingsForRole(roleId={})", roleId);
        if (roleId == null || roleId.trim().isEmpty()) {
            log.warn("Attempted to remove user mappings for empty roleId.");
            return;
        }
        try {
            Long roleIdLong = Long.valueOf(roleId);
            EntityTransaction tx = em.getTransaction();
            tx.begin();
            // Delete entries from the users_roles join table for the given role ID
            em.createNativeQuery("DELETE FROM users_roles WHERE roles_id = :roleId")
                    .setParameter("roleId", roleIdLong) // Assuming roles_id column and Long ID
                    .executeUpdate();
            tx.commit();
        } catch (NumberFormatException e) {
            log.warn("Invalid role ID format for removeUserMappingsForRole: {}", roleId, e);
        } catch (Exception e) {
            log.error("Failed to remove user mappings for role {}: {}", roleId, e.getMessage(), e);
            throw e; // Re-throw exception
        }
    }

    // Implemented based on CustomUserStorageProvider needs (assignment removal)
    public void deleteAllAssignmentsForRealmRoles(String realmId) {
        log.debug("UserRepository.deleteAllAssignmentsForRealmRoles(realmId={})", realmId);
        if (realmId == null || realmId.trim().isEmpty()) {
            log.warn("Attempted to delete realm role assignments with empty realmId.");
            return;
        }
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            // Delete entries from users_roles where the role is a realm role in the specified realm
            em.createNativeQuery("DELETE FROM users_roles ur WHERE ur.roles_id IN (SELECT r.id FROM roles r WHERE r.realmId = :realmId AND r.clientId IS NULL)")
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
        log.debug("UserRepository.deleteAllAssignmentsForClientRoles(clientId={})", clientId);
        if (clientId == null || clientId.trim().isEmpty()) {
            log.warn("Attempted to delete client role assignments with empty clientId.");
            return;
        }
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            // Delete entries from users_roles where the role is a client role for the specified client
            em.createNativeQuery("DELETE FROM users_roles ur WHERE ur.roles_id IN (SELECT r.id FROM roles r WHERE r.clientId = :clientId)")
                    .setParameter("clientId", clientId) // Assuming Role entity has clientId
                    .executeUpdate();
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            log.error("Failed to delete all client role assignments for client {}: {}", clientId, e.getMessage(), e);
            throw e;
        }
    }


    // Implemented based on CustomUserStorageProvider needs (for grantToAllUsers)
    public List<UserEntity> getAllUsers(String realmId, Integer first, Integer max) {
        log.debug("UserRepository.getAllUsers(realmId={}, first={}, max={})", realmId, first, max);
        if (realmId == null || realmId.trim().isEmpty()) {
            return new ArrayList<>();
        }
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<UserEntity> cq = cb.createQuery(UserEntity.class);
        Root<UserEntity> root = cq.from(UserEntity.class);
        List<Predicate> predicates = new ArrayList<>();

        // Filter by realm (assuming UserEntity has realmId)
        predicates.add(cb.equal(root.get("realmId"), realmId));

        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        // Optional: Add ordering, e.g., cq.orderBy(cb.asc(root.get("id")));

        TypedQuery<UserEntity> query = em.createQuery(cq);

        if (first != null) {
            query.setFirstResult(first);
        }
        if (max != null) {
            query.setMaxResults(max);
        }

        return query.getResultList();
    }

    /**
     * Finds a list of UserEntities that are directly associated with a given Keycloak Role ID.
     *
     * @param realmId       The Keycloak realm ID.
     * @param keycloakRoleId The Keycloak ID of the role.
     * @param firstResult   The index of the first result to retrieve (for pagination).
     * @param maxResults    The maximum number of results to retrieve (for pagination).
     * @return A list of UserEntities associated with the role, within the specified range.
     */
    public List<UserEntity> findUsersByRealmAndRole(String realmId, String keycloakRoleId, Integer firstResult, Integer maxResults) {
        log.debug("UserRepository.findUsersByRealmAndRole(realmId={}, keycloakRoleId={}, firstResult={}, maxResults={})", realmId, keycloakRoleId, firstResult, maxResults);
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<UserEntity> cq = cb.createQuery(UserEntity.class);
            Root<UserEntity> root = cq.from(UserEntity.class);

            Join<UserEntity, Group> rolesJoin = root.join("roles", JoinType.LEFT);
            // Join with the roles collection

            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(rolesJoin.get("realmId"), realmId)); // Filter by realm
            predicates.add(cb.equal(rolesJoin.get("keycloakId"), keycloakRoleId)); // Filter by the external group ID via the join

            cq.where(cb.and(predicates.toArray(new Predicate[0])));

            TypedQuery<UserEntity> query = em.createQuery(cq);
            if (firstResult != null) {
                query.setFirstResult(firstResult);
            }
            if (maxResults != null) {
                query.setMaxResults(maxResults);
            }
            return query.getResultList();
        } catch (Exception e) {
            log.error("Error finding users by realm {} and role {}: {}", realmId, keycloakRoleId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Finds a list of UserEntities that are directly associated with a given Keycloak Role ID (without pagination).
     *
     * @param realmId       The Keycloak realm ID.
     * @param keycloakRoleId The Keycloak ID of the role.
     * @return A list of UserEntities associated with the role.
     */
    public List<UserEntity> findUsersByRealmAndRole(String realmId, String keycloakRoleId) {
        return findUsersByRealmAndRole(realmId, keycloakRoleId, null, null);
    }

    /**
     * Counts the number of UserEntities that are directly associated with a given Keycloak Role ID.
     *
     * @param realmId       The Keycloak realm ID.
     * @param keycloakRoleId The Keycloak ID of the role.
     * @return The total number of users associated with the role.
     */
    public long countUsersByRealmAndRole(String realmId, String keycloakRoleId) {
        log.debug("UserRepository.countUsersByRealmAndRole(realmId={}, keycloakRoleId={})", realmId, keycloakRoleId);
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Long> cq = cb.createQuery(Long.class);
            Root<UserEntity> root = cq.from(UserEntity.class);
            // Join with the roles collection
            Join<UserEntity, Group> rolesJoin = root.join("roles", JoinType.LEFT);

            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(rolesJoin.get("realmId"), realmId));
            predicates.add(cb.equal(rolesJoin.get("r").get("keycloakId"), keycloakRoleId));

            cq.where(cb.and(predicates.toArray(new Predicate[0])));

            cq.select(cb.count(root));
            return em.createQuery(cq).getSingleResult();
        } catch (Exception e) {
            log.error("Error counting users by realm {} and role {}: {}", realmId, keycloakRoleId, e.getMessage(), e);
            return 0;
        }
    }

}
