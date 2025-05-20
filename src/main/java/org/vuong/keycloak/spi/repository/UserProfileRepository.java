package org.vuong.keycloak.spi.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vuong.keycloak.spi.entity.UserProfile;
import org.vuong.keycloak.spi.entity.UserEntity; // Needed for realmId filtering in search

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional; // Using Optional for find methods for better null handling

public class UserProfileRepository {

    private static final Logger log = LoggerFactory.getLogger(UserProfileRepository.class);
    private final EntityManager em;

    public UserProfileRepository(EntityManager em) {
        this.em = em;
    }

    /**
     * Finds a user profile by the internal ID (String representation of Long) of its associated UserEntity.
     * This is the primary foreign key link.
     *
     * @param userId The internal ID (as String) of the linked UserEntity.
     * @return An Optional containing the UserProfile if found.
     */
    public Optional<UserProfile> findByUserId(String userId) {
        log.debug("UserProfileRepository.findByUserId({})", userId);
        if (userId == null || userId.trim().isEmpty()) {
            return Optional.empty();
        }
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<UserProfile> cq = cb.createQuery(UserProfile.class);
            Root<UserProfile> root = cq.from(UserProfile.class);
            cq.where(cb.equal(root.get("userId"), userId));
            return Optional.of(em.createQuery(cq).getSingleResult());
        } catch (NoResultException e) {
            log.debug("UserProfile not found for userId: {}", userId);
            return Optional.empty();
        }
    }

    /**
     * Finds a UserProfile by its own internal primary key ID.
     *
     * @param id The internal Long ID of the UserProfile.
     * @return An Optional containing the UserProfile if found.
     */
    public Optional<UserProfile> getById(Long id) {
        log.debug("UserProfileRepository.getById({})", id);
        if (id == null) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(em.find(UserProfile.class, id));
        } catch (Exception e) {
            log.error("Error finding UserProfile by ID {}: {}", id, e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Finds a UserProfile by the Keycloak ID of its associated UserEntity.
     * This requires joining with the UserEntity table.
     * Assumes UserEntity is also managed by JPA in the same persistence unit.
     *
     * @param userKeycloakId The Keycloak UUID of the associated UserEntity.
     * @return An Optional containing the UserProfile if found.
     */
    public Optional<UserProfile> findByUserKeycloakId(String userKeycloakId) {
        log.debug("UserProfileRepository.findByUserKeycloakId({})", userKeycloakId);
        if (userKeycloakId == null || userKeycloakId.trim().isEmpty()) {
            return Optional.empty();
        }
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<UserProfile> cq = cb.createQuery(UserProfile.class);
            Root<UserProfile> profile = cq.from(UserProfile.class);
            // Join UserEntity using the userId field in UserProfile
            Join<UserProfile, UserEntity> user = profile.join("users", JoinType.INNER); // Assuming 'userEntity' is a mapped field in UserProfile, or you map directly via UserEntity's ID
            // If UserProfile does not have a direct 'userEntity' field mapped, you need to join manually using root UserEntity:
            // Root<UserEntity> user = cq.from(UserEntity.class);
            // predicates.add(cb.equal(profile.get("userId"), user.get("id").as(String.class))); // Linking predicate

            cq.where(cb.equal(user.get("keycloakId"), userKeycloakId)); // Assuming UserEntity has keycloakId
            return Optional.of(em.createQuery(cq).getSingleResult());
        } catch (NoResultException e) {
            log.debug("UserProfile not found for Keycloak user ID: {}", userKeycloakId);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error finding UserProfile by Keycloak user ID {}: {}", userKeycloakId, e.getMessage(), e);
            throw e;
        }
    }


    /**
     * Performs a dynamic search for UserProfiles based on provided attributes.
     * This method builds a Criteria API query similar to UserRepository's search.
     *
     * @param realmId               The ID of the realm (optional, for filtering users within a realm).
     * @param userProfileAttributes A map of attribute names and their values to search for.
     * Supported keys could be: "firstName", "lastName", "phone", "address", "avatarUrl", "enabled".
     * @param firstResult           Offset for pagination.
     * @param maxResults            Maximum number of results to return.
     * @return A list of matching UserProfile entities.
     */
    public List<UserProfile> search(String realmId, Map<String, String> userProfileAttributes, Integer firstResult, Integer maxResults) {
        log.debug("UserProfileRepository.search(realmId={}, attributes={}, first={}, max={})", realmId, userProfileAttributes, firstResult, maxResults);
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<UserProfile> cq = cb.createQuery(UserProfile.class);
        Root<UserProfile> profile = cq.from(UserProfile.class);
        List<Predicate> predicates = new ArrayList<>();

        // If realmId is provided, join with UserEntity to filter profiles by the associated user's realm
        if (realmId != null && !realmId.trim().isEmpty()) {
            Join<UserProfile, UserEntity> user = profile.join("userEntity", JoinType.INNER); // Assumes a 'userEntity' relationship is mapped in UserProfile
            predicates.add(cb.equal(user.get("realmId"), realmId));
        }

        userProfileAttributes.forEach((key, value) -> {
            if (value != null && !value.isEmpty()) {
                switch (key) {
                    case "firstName":
                        predicates.add(cb.like(cb.lower(profile.get("firstName")), "%" + value.toLowerCase() + "%"));
                        break;
                    case "lastName":
                        predicates.add(cb.like(cb.lower(profile.get("lastName")), "%" + value.toLowerCase() + "%"));
                        break;
                    case "phone":
                        predicates.add(cb.like(profile.get("phone"), "%" + value + "%"));
                        break;
                    case "address":
                        predicates.add(cb.like(cb.lower(profile.get("address")), "%" + value.toLowerCase() + "%"));
                        break;
                    case "avatarUrl":
                        predicates.add(cb.like(profile.get("avatarUrl"), "%" + value + "%"));
                        break;
                    case "enabled":
                        try {
                            predicates.add(cb.equal(profile.get("enabled"), Boolean.parseBoolean(value)));
                        } catch (IllegalArgumentException e) {
                            log.warn("Invalid boolean value for 'enabled' in search: {}", value);
                        }
                        break;
                    default:
                        log.warn("Unsupported attribute '{}' for UserProfile search.", key);
                        break;
                }
            }
        });

        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        // Optional: Add ordering, e.g., cq.orderBy(cb.asc(profile.get("firstName")));

        TypedQuery<UserProfile> query = em.createQuery(cq);

        if (firstResult != null) {
            query.setFirstResult(firstResult);
        }
        if (maxResults != null) {
            query.setMaxResults(maxResults);
        }

        return query.getResultList();
    }

    /**
     * Ensures a UserProfile exists for a given UserEntity's internal ID.
     * If a profile does not exist for the userId, it creates a new one with default values.
     * This acts as a "sync" for the UserProfile's existence linked to a UserEntity,
     * similar to how a `syncGroup` might ensure a Group entity exists.
     *
     * @param userInternalId The internal ID (String representation of Long) of the UserEntity.
     * @param realmId The ID of the realm.
     * @return The existing or newly created UserProfile.
     */
    public UserProfile syncUserProfile(String userInternalId, String realmId) {
        log.debug("UserProfileRepository.syncUserProfile(userInternalId={})", userInternalId);
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Optional<UserProfile> existingProfile = findByUserId(userInternalId);

            if (existingProfile.isPresent()) {
                log.debug("Existing user profile found for userInternalId: {}", userInternalId);
                tx.commit(); // No changes made, just commit transaction
                return existingProfile.get();
            } else {
                log.info("Creating new user profile for userInternalId: {}", userInternalId);
                UserProfile newProfile = new UserProfile();
//                newProfile.setUserId(userInternalId);
                newProfile.setCreatedAt(Instant.now());
                // Optionally, set createdBy, updatedBy if you have context (e.g., from KeycloakSession)
                 newProfile.setCreatedBy("keycloak-admin");

                em.persist(newProfile); // Persist the new profile
                tx.commit();
                log.info("Successfully created new user profile for userInternalId: {}", userInternalId);
                return newProfile;
            }
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            log.error("Failed to sync/create user profile for userInternalId {}: {}", userInternalId, e.getMessage(), e);
            throw e; // Re-throw the exception
        }
    }

    /**
     * Saves or updates a UserProfile entity.
     *
     * @param userProfile The UserProfile entity to save.
     * @return The managed (persisted or merged) UserProfile entity.
     */
    public UserProfile save(UserProfile userProfile) {
//        log.debug("UserProfileRepository.save({})", userProfile != null ? userProfile.getUserId() : "null");
        EntityTransaction tx = em.getTransaction();
        UserProfile savedProfile = null;
        try {
            tx.begin();
            if (userProfile.getId() == null) {
                em.persist(userProfile);
                savedProfile = userProfile;
            } else {
                savedProfile = em.merge(userProfile);
            }
            tx.commit();
//            log.debug("Successfully saved user profile for userId {}", savedProfile != null ? savedProfile.getUserId() : "null");
            return savedProfile;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
//            log.error("Failed to save user profile for userId {}: {}", userProfile != null ? userProfile.getUserId() : "null", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Deletes a UserProfile by the internal ID of its associated UserEntity.
     *
     * @param userId The internal ID (as String) of the linked UserEntity.
     */
    public void deleteByUserId(String userId) {
        log.debug("UserProfileRepository.deleteByUserId({})", userId);
        if (userId == null || userId.trim().isEmpty()) {
            log.warn("Attempted to delete UserProfile with empty userId.");
            return;
        }
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            // Using a Criteria DELETE query for efficiency
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaDelete<UserProfile> cd = cb.createCriteriaDelete(UserProfile.class);
            Root<UserProfile> root = cd.from(UserProfile.class);
            cd.where(cb.equal(root.get("userId"), userId));
            int deletedCount = em.createQuery(cd).executeUpdate();
            tx.commit();
            log.debug("Deleted {} UserProfiles for userId: {}", deletedCount, userId);
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            log.error("Failed to delete UserProfile for userId {}: {}", userId, e.getMessage(), e);
            throw e;
        }
    }
}