package org.vuong.keycloak.spi;

import jakarta.persistence.EntityManager;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.storage.group.GroupStorageProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vuong.keycloak.spi.config.JpaProvider; // Assuming JpaProvider is used for EntityManager
import org.vuong.keycloak.spi.repository.GroupRepository;
import org.vuong.keycloak.spi.repository.RoleRepository; // Include if GroupProvider needs RoleRepository

import java.util.List;

// Note: This factory should be registered with Keycloak separately from the User provider factory.
public class CustomGroupStorageProviderFactory implements
        GroupStorageProviderFactory<CustomGroupStorageProvider> {

    private static final Logger log = LoggerFactory.getLogger(CustomGroupStorageProviderFactory.class);

    // Define configuration properties if needed, e.g., for database connection
    // For simplicity, reusing JpaProvider which assumes external configuration (hibernate.cfg.xml)
    // If you need admin console configuration, uncomment and adapt the properties builder.
    private static final List<ProviderConfigProperty> configMetadata = ProviderConfigurationBuilder.create()
            // Add any necessary configuration properties specific to the group provider
            // .property().name("some.property").type(ProviderConfigProperty.STRING_TYPE).label("Some Property").add()
            .build();


    @Override
    public CustomGroupStorageProvider create(KeycloakSession session, ComponentModel model) {
        log.info("CustomGroupStorageProviderFactory.create()");
        // Obtain EntityManager - reuse your JpaProvider utility
        EntityManager em = JpaProvider.getSession(); // Assuming getSession returns EntityManager or Session that can be adapted

        // Instantiate repositories needed by the Group provider
        GroupRepository groupRepository = new GroupRepository(em);
        RoleRepository roleRepository = new RoleRepository(em); // Include if group-role mapping is managed here

        // Pass repositories to the provider
        return new CustomGroupStorageProvider(session, model, groupRepository, roleRepository);
    }

    @Override
    public String getId() {
        log.info("CustomGroupStorageProviderFactory.getId()");
        return "group-jpa-provider"; // Unique ID for this provider type
    }

    @Override
    public String getHelpText() {
        return "JPA Group Storage Provider";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configMetadata;
    }
}