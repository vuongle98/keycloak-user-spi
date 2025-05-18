package org.vuong.keycloak.spi.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.vuong.keycloak.spi.entity.UserEntity;

import java.util.ArrayList;
import java.util.List;

public class UserRepository {

    private final EntityManager em;

    public UserRepository(EntityManager em) {
        this.em = em;
    }

    public UserEntity findByUsername(String username) {
        try {
            TypedQuery<UserEntity> query = em.createQuery(
                    "SELECT u FROM users u WHERE u.username = :username", UserEntity.class
            );
            query.setParameter("username", username);
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public UserEntity findByEmail(String email) {
        try {
            TypedQuery<UserEntity> query = em.createQuery(
                    "SELECT u FROM users u WHERE u.email = :email", UserEntity.class
            );
            query.setParameter("email", email);
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public List<UserEntity> searchByUsername(String username) {
        try {
            TypedQuery<UserEntity> query = em.createQuery(
                    "SELECT u FROM users u WHERE u.username like :username", UserEntity.class
            );
            query.setParameter("username", username);
            return query.getResultList();
        } catch (NoResultException e) {
            return null;
        }
    }

    public List<UserEntity> searchByEmail(String email) {
        try {
            TypedQuery<UserEntity> query = em.createQuery(
                    "SELECT u FROM users u WHERE u.email like :email", UserEntity.class
            );
            query.setParameter("email", email);
            return query.getResultList();
        } catch (NoResultException e) {
            return null;
        }
    }

    public List<UserEntity> searchByLock(Boolean locked) {
        try {
            TypedQuery<UserEntity> query = em.createQuery(
                    "SELECT u FROM users u WHERE u.locked = :locked", UserEntity.class
            );
            query.setParameter("locked", locked);
            return query.getResultList();
        } catch (NoResultException e) {
            return null;
        }
    }

    public UserEntity getById(String id) {
        try {
            TypedQuery<UserEntity> query = em.createQuery(
                    "SELECT u FROM users u WHERE u.id = :id", UserEntity.class
            );
            query.setParameter("id", id);
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public List<UserEntity> getUsers() {
        try {
            TypedQuery<UserEntity> query = em.createQuery(
                    "SELECT u FROM users u", UserEntity.class
            );
            return query.getResultList();
        } catch (NoResultException e) {
            return List.of();
        }
    }

    public int countUsers() {
        try {
            TypedQuery<Long> query = em.createQuery(
                    "SELECT COUNT(u) FROM users u", Long.class
            );
            return query.getSingleResult().intValue();
        } catch (NoResultException e) {
            return 0;
        }
    }

    public List<UserEntity> search(String search, String username, String email, Integer firstResult, Integer maxResults) {
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
            predicates.add(cb.or(
                    cb.like(cb.lower(root.get("username")), "%" + search.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("email")), "%" + search.toLowerCase() + "%")
            ));
        }

        cq.where(predicates.toArray(new Predicate[0]));

        TypedQuery<UserEntity> query = em.createQuery(cq);

        if (firstResult != null) {
            query.setFirstResult(firstResult);
        }
        if (maxResults != null) {
            query.setMaxResults(maxResults);
        }

        return query.getResultList();
    }

    public List<UserEntity> findByAttribute(String attrName, String attrValue) {
        // Giả sử có cột attributes kiểu Map trong UserEntity
        String jpql = "SELECT u FROM users u JOIN u.attributes attr WHERE KEY(attr) = :attrName AND VALUE(attr) = :attrValue";
        TypedQuery<UserEntity> query = em.createQuery(jpql, UserEntity.class);
        query.setParameter("attrName", attrName);
        query.setParameter("attrValue", attrValue);
        return query.getResultList();
    }

//    public List<UserEntity> findByGroupId(String groupId, Integer firstResult, Integer maxResults) {
//        CriteriaBuilder cb = em.getCriteriaBuilder();
//        CriteriaQuery<UserEntity> cq = cb.createQuery(UserEntity.class);
//        Root<UserEntity> userRoot = cq.from(UserEntity.class);
//
//        Join<UserEntity, GroupEntity> groupJoin = userRoot.join("groups"); // tên thuộc tính groups trong UserEntity
//
//        cq.where(cb.equal(groupJoin.get("id"), groupId));
//
//        TypedQuery<UserEntity> query = entityManager.createQuery(cq);
//
//        if (firstResult != null) {
//            query.setFirstResult(firstResult);
//        }
//        if (maxResults != null) {
//            query.setMaxResults(maxResults);
//        }
//
//        return query.getResultList();
//    }
}