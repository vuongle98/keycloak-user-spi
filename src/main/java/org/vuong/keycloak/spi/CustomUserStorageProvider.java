package org.vuong.keycloak.spi;

import jakarta.persistence.EntityManager;
import org.keycloak.authentication.CredentialValidator;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserRegistrationProvider;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryProvider;
import org.slf4j.LoggerFactory;
import org.vuong.keycloak.spi.config.CustomUserAdapter;
import org.vuong.keycloak.spi.entity.UserEntity;
import org.vuong.keycloak.spi.repository.UserRepository;
import org.vuong.keycloak.spi.util.PasswordUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.slf4j.Logger;


public class CustomUserStorageProvider implements
        UserStorageProvider,
        UserLookupProvider,
        UserQueryProvider,
        CredentialInputValidator {

    private static final Logger logger = LoggerFactory.getLogger(CustomUserStorageProvider.class);


    private final KeycloakSession session;
    private final UserRepository userRepository;

    public CustomUserStorageProvider(KeycloakSession session, UserRepository userRepository) {
        this.session = session;
        this.userRepository = userRepository;
    }

    @Override
    public UserModel getUserById(RealmModel realm, String id) {
        logger.info("getUserById: id={}", id);
        UserEntity entity = userRepository.getById(id);
        if (entity == null) return null;

        UserModel user = session.users().getUserByUsername(realm, entity.getUsername());
        if (user == null) {
            user = session.users().addUser(realm, entity.getId().toString(), entity.getUsername(), false, false);
            user.setEnabled(true);
        }
        return user;
    }

    @Override
    public UserModel getUserByUsername(RealmModel realm, String username) {
        logger.info("getUserByUsername: username={}", username);
        UserEntity entity = userRepository.findByUsername(username);
        if (entity == null) return null;

        UserModel user = session.users().getUserByUsername(realm, entity.getUsername());
        if (user == null) {
            user = session.users().addUser(realm, entity.getId().toString(), entity.getUsername(), false, false);
            user.setEnabled(true);
        }
        return user;
    }

    @Override
    public UserModel getUserByEmail(RealmModel realm, String email) {
        logger.info("getUserByEmail: email={}", email);
        UserEntity entity = userRepository.findByEmail(email);
        if (entity == null) return null;

        UserModel user = session.users().getUserByEmail(realm, entity.getEmail());
        if (user == null) {
            user = session.users().addUser(realm, entity.getId().toString(), entity.getUsername(), false, false);
            user.setEnabled(true);
        }
        return user;
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return credentialType.equalsIgnoreCase(PasswordCredentialModel.TYPE);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        String password = userRepository.findByUsername(user.getUsername()).getPassword();
        return credentialType.equalsIgnoreCase(PasswordCredentialModel.TYPE) && password != null;
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput credentialInput) {
        if (!supportsCredentialType(credentialInput.getType())) return false;

        UserEntity entity = userRepository.findByUsername(user.getUsername());
        if (entity == null) return false;

        return PasswordUtil.verifyPassword(credentialInput.getChallengeResponse(), entity.getPassword());
    }

    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, Map<String, String> params, Integer firstResult, Integer maxResults) {

        Map<String, String> normalized = normalizeParams(params);

        String username = normalized.get("username");
        String email = normalized.get("email");
        String search = normalized.get("search");

        return userRepository.search(search, username, email, firstResult, maxResults)
                .stream()
                .map(userEntity -> new CustomUserAdapter(session, realm, userEntity));
    }

    @Override
    public Stream<UserModel> getGroupMembersStream(RealmModel realm, GroupModel group, Integer firstResult, Integer maxResults) {
        return Stream.empty();
    }

    @Override
    public Stream<UserModel> searchForUserByUserAttributeStream(RealmModel realm, String attrName, String attrValue) {
        if (attrName == null || attrValue == null) {
            return Stream.empty();
        }

        List<UserEntity> users;

        switch (attrName) {
            case "username":
                users = userRepository.searchByUsername(attrValue);
                break;
            case "email":
                users = userRepository.searchByEmail(attrValue);
                break;
            case "locked":
                boolean locked;
                try {
                    locked = Boolean.parseBoolean(attrValue);
                } catch (Exception e) {
                    return Stream.empty();
                }
                users = userRepository.searchByLock(locked);
                break;
            default:
                // Không hỗ trợ attribute khác
                return Stream.empty();
        }

        return users.stream().map(user -> new CustomUserAdapter(session, realm, user));
    }


    @Override
    public void close() {

    }

    private Map<String, String> normalizeParams(Map<String, String> params) {
        Map<String, String> normalized = new HashMap<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            if (key.contains("username")) normalized.put("username", entry.getValue());
            if (key.contains("email")) normalized.put("email", entry.getValue());
            if (key.contains("search")) normalized.put("search", entry.getValue());
        }
        return normalized;
    }

//    @Override
//    public UserModel addUser(RealmModel realm, String username) {
//        return null;
//    }
//
//    @Override
//    public boolean removeUser(RealmModel realm, UserModel user) {
//        return false;
//    }
}
