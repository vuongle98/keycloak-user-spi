package org.vuong.keycloak.spi;

import jakarta.persistence.EntityManager;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.storage.UserStorageProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vuong.keycloak.spi.config.JpaProvider;
import org.vuong.keycloak.spi.repository.UserRepository;

public class CustomUserStorageProviderFactory implements
        UserStorageProviderFactory<CustomUserStorageProvider> {

    private static final Logger log = LoggerFactory.getLogger(CustomUserStorageProviderFactory.class);

    @Override
    public CustomUserStorageProvider create(KeycloakSession session, ComponentModel model) {
        log.info("CustomUserStorageProviderFactory.create()");
        EntityManager em = JpaProvider.getSession();
        UserRepository userRepository = new UserRepository(em);
        return new CustomUserStorageProvider(session, userRepository);
    }

    @Override
    public String getId() {
        log.info("CustomUserStorageProviderFactory.getId()");
        return "user-jpa-provider";
    }

    @Override
    public void init(org.keycloak.Config.Scope config) {}

    @Override
    public void postInit(KeycloakSessionFactory factory) {}

    @Override
    public void close() {}
}
