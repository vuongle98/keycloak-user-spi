package org.vuong.keycloak.spi;

import jakarta.persistence.EntityManager;
import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import jakarta.persistence.spi.ClassTransformer;
import jakarta.persistence.spi.PersistenceUnitInfo;
import jakarta.persistence.spi.PersistenceUnitTransactionType;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder; // Import ProviderConfigurationBuilder
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderFactory;
import org.keycloak.storage.group.GroupLookupProvider; // Keep if needed for imports/references
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vuong.keycloak.spi.config.JpaProvider;
import org.vuong.keycloak.spi.entity.UserEntity; // Keep if needed for imports/references
import org.vuong.keycloak.spi.repository.GroupRepository; // Keep if needed for imports/references
import org.vuong.keycloak.spi.repository.RoleRepository; // Keep if needed for imports/references
import org.vuong.keycloak.spi.repository.UserRepository;
// Removed imports for GroupRepository and RoleRepository

import javax.sql.DataSource;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.*;

public class CustomUserStorageProviderFactory implements
        UserStorageProviderFactory<CustomUserStorageProvider> {

    private static final Logger log = LoggerFactory.getLogger(CustomUserStorageProviderFactory.class);

    // Correct way to define an empty list of configuration properties
    protected static final List<ProviderConfigProperty> configMetadata = ProviderConfigurationBuilder.create().build();

    // Add any user-specific config properties here if needed
    // static {
    //     configMetadata.add(new ProviderConfigProperty("some.user.property", "Some User Property", "Description", ProviderConfigProperty.STRING_TYPE, ""));
    // }


    @Override
    public CustomUserStorageProvider create(KeycloakSession session, ComponentModel model) {
        log.info("CustomUserStorageProviderFactory.create()");
        // Obtain EntityManager - reuse your JpaProvider utility
        EntityManager em = JpaProvider.getSession(); // Assuming getSession returns EntityManager or Session that can be adapted

        // Instantiate the repositories needed by the User provider
        UserRepository userRepository = new UserRepository(em);
        RoleRepository roleRepository = new RoleRepository(em); // Needed for user-role assignment methods
        GroupRepository groupRepository = new GroupRepository(em); // Needed for user-group assignment methods

        // Pass repositories to the provider
        return new CustomUserStorageProvider(session, model, userRepository, roleRepository, groupRepository);
    }

    // The getPersistenceUnitInfo method is typically needed if the factory is responsible for creating the EntityManagerFactory/PersistenceUnit.
    // If JpaProvider handles this based on a separate configuration (like persistence.xml), this method might not be necessary in the factory.
    // If you uncomment it, ensure it's fully implemented and correctly provides the PersistenceUnitInfo.
//    private PersistenceUnitInfo getPersistenceUnitInfo(String name) {
//       // ... (implementation as before)
//    }

    @Override
    public String getId() {
        log.info("CustomUserStorageProviderFactory.getId()");
        return "user-jpa-provider"; // Unique ID for this provider type
    }

    @Override
    public String getHelpText() {
        return "JPA User Storage Provider";
    }

    @Override // Uncommented and returning the empty list
    public List<ProviderConfigProperty> getConfigProperties() {
        return configMetadata;
    }
}