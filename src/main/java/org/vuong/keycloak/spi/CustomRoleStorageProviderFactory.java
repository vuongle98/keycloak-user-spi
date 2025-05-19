package org.vuong.keycloak.spi;

import jakarta.persistence.EntityManager;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.storage.role.RoleStorageProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vuong.keycloak.spi.config.JpaProvider; // Assuming JpaProvider is used for EntityManager
import org.vuong.keycloak.spi.repository.RoleRepository;

import java.util.ArrayList;
import java.util.List;

// Factory for the Role Storage Provider
public class CustomRoleStorageProviderFactory implements
        RoleStorageProviderFactory<CustomRoleStorageProvider> {

    private static final Logger log = LoggerFactory.getLogger(CustomRoleStorageProviderFactory.class);

    // Correct way to define an empty list of configuration properties
    private static final List<ProviderConfigProperty> configMetadata = ProviderConfigurationBuilder.create().build();


    @Override
    public CustomRoleStorageProvider create(KeycloakSession session, ComponentModel model) {
        log.info("CustomRoleStorageProviderFactory.create()");
        // Obtain EntityManager - reuse your JpaProvider utility
        EntityManager em = JpaProvider.getSession(); // Assuming getSession returns EntityManager or Session that can be adapted

        // Instantiate only the repository needed by the Role provider
        RoleRepository roleRepository = new RoleRepository(em);

        // Pass repository to the provider
        return new CustomRoleStorageProvider(session, model, roleRepository);
    }

    @Override
    public String getId() {
        log.info("CustomRoleStorageProviderFactory.getId()");
        return "role-jpa-provider"; // Unique ID for this provider type
    }

    @Override
    public String getHelpText() {
        return "JPA Role Storage Provider";
    }

    @Override // Uncommented and returning the empty list
    public List<ProviderConfigProperty> getConfigProperties() {
        return configMetadata;
    }
}