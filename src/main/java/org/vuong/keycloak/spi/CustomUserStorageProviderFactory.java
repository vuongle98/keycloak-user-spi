package org.vuong.keycloak.spi;

import jakarta.persistence.EntityManager;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.storage.UserStorageProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vuong.keycloak.spi.config.JpaProvider;

import java.util.List;

public class CustomUserStorageProviderFactory implements
        UserStorageProviderFactory<CustomUserStorageProvider> {

    private static final Logger log = LoggerFactory.getLogger(CustomUserStorageProviderFactory.class);

    public static final String PROVIDER_ID = "user-jpa-provider";

    // Configuration property keys
    public static final String CONFIG_KEY_JDBC_URL = "jdbcUrl";
    public static final String CONFIG_KEY_DB_USERNAME = "dbUsername";
    public static final String CONFIG_KEY_DB_PASSWORD = "dbPassword";
    public static final String CONFIG_KEY_DIALECT = "dialect";

    private static final List<ProviderConfigProperty> configMetadata;

    static {
        configMetadata = ProviderConfigurationBuilder.create()
                .property()
                .name(CONFIG_KEY_JDBC_URL)
                .label("JDBC Connection URL")
                .helpText("The JDBC URL to connect to the database.")
                .type(ProviderConfigProperty.STRING_TYPE)
                .required(true)
                .add()
                .property()
                .name(CONFIG_KEY_DB_USERNAME)
                .label("Database Username")
                .helpText("The username to connect to the database.")
                .type(ProviderConfigProperty.STRING_TYPE)
                .required(true)
                .add()
                .property()
                .name(CONFIG_KEY_DB_PASSWORD)
                .label("Database Password")
                .helpText("The password to connect to the database.")
                .type(ProviderConfigProperty.PASSWORD)
                .required(true)
                .add()
                .property()
                .name(CONFIG_KEY_DIALECT)
                .label("JPA Dialect")
                .helpText("The JPA dialect to use (e.g., org.hibernate.dialect.PostgreSQLDialect).")
                .type(ProviderConfigProperty.STRING_TYPE)
                .required(true)
                .add()
                .build();
    }

    @Override
    public CustomUserStorageProvider create(KeycloakSession session, ComponentModel model) {
        log.info("CustomUserStorageProviderFactory.create() - Component Model ID: {}", model.getId());

        // Retrieve configuration from the ComponentModel
        String jdbcUrl = model.getConfig().getFirst(CONFIG_KEY_JDBC_URL);
        String dbUsername = model.getConfig().getFirst(CONFIG_KEY_DB_USERNAME);
        String dbPassword = model.getConfig().getFirst(CONFIG_KEY_DB_PASSWORD);
        String dialect = model.getConfig().getFirst(CONFIG_KEY_DIALECT);

        log.debug("Configuration - JDBC URL: {}, Username: {}, Dialect: {}", jdbcUrl, dbUsername, dialect);

        // Initialize your JpaProvider with the dynamic configuration
        EntityManager em = JpaProvider.getEntityManager(jdbcUrl, dbUsername, dbPassword, dialect);

        // Pass the EntityManager and the ComponentModel (containing the config) to the provider
        return new CustomUserStorageProvider(session, model, em);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getHelpText() {
        return "JPA User Storage Provider with Dynamic Database Configuration";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configMetadata;
    }
}