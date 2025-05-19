package org.vuong.keycloak.spi;

import jakarta.persistence.EntityManager;
import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import jakarta.persistence.spi.ClassTransformer;
import jakarta.persistence.spi.PersistenceUnitInfo;
import jakarta.persistence.spi.PersistenceUnitTransactionType;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.storage.UserStorageProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vuong.keycloak.spi.config.JpaProvider;
import org.vuong.keycloak.spi.entity.UserEntity;
import org.vuong.keycloak.spi.repository.GroupRepository;
import org.vuong.keycloak.spi.repository.RoleRepository;
import org.vuong.keycloak.spi.repository.UserRepository;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.*;

public class CustomUserStorageProviderFactory implements
        UserStorageProviderFactory<CustomUserStorageProvider> {

    private static final Logger log = LoggerFactory.getLogger(CustomUserStorageProviderFactory.class);

    Map<String, String> properties;
    Map<String, EntityManager> entityManagerFactories = new HashMap<>();

    protected static final List<ProviderConfigProperty> configMetadata = new ArrayList<>();

    public static final int PORT_LIMIT = 65535;

    public static final String DB_CONNECTION_NAME_KEY = "db:connectionName";
    public static final String DB_HOST_KEY = "db:host";
    public static final String DB_DATABASE_KEY = "db:database";
    public static final String DB_USERNAME_KEY = "db:username";
    public static final String DB_PASSWORD_KEY = "db:password";
    public static final String DB_PORT_KEY = "db:port";

//    static {
//        configMetadata = ProviderConfigurationBuilder.create()
//                // Connection Name
//                .property().name(DB_CONNECTION_NAME_KEY)
//                .type(ProviderConfigProperty.STRING_TYPE)
//                .label("Connection Name")
//                .defaultValue("")
//                .helpText("Name of the connection, can be chosen individually. Enables connection sharing between providers if the same name is provided. Overrides currently saved connection properties.")
//                .add()
//
//                // Connection Host
//                .property().name(DB_HOST_KEY)
//                .type(ProviderConfigProperty.STRING_TYPE)
//                .label("Database Host")
//                .defaultValue("localhost")
//                .helpText("Host of the connection")
//                .add()
//
//                // Connection Database
//                .property().name(DB_DATABASE_KEY)
//                .type(ProviderConfigProperty.STRING_TYPE)
//                .label("Database Name")
//                .add()
//
//                // DB Username
//                .property().name(DB_USERNAME_KEY)
//                .type(ProviderConfigProperty.STRING_TYPE)
//                .label("Database Username")
//                .defaultValue("user")
//                .add()
//
//                // DB Password
//                .property().name(DB_PASSWORD_KEY)
//                .type(ProviderConfigProperty.PASSWORD)
//                .label("Database Password")
//                .defaultValue("PASSWORD")
//                .add()
//
//                // DB Port
//                .property().name(DB_PORT_KEY)
//                .type(ProviderConfigProperty.STRING_TYPE)
//                .label("Database Port")
//                .defaultValue("5432")
//                .add()
//                .build();
//    }
//
//    @Override
//    public List<ProviderConfigProperty> getConfigProperties() {
//        return configMetadata;
//    }

    @Override
    public CustomUserStorageProvider create(KeycloakSession session, ComponentModel model) {
        log.info("CustomUserStorageProviderFactory.create()");
        EntityManager em = JpaProvider.getSession();
        UserRepository userRepository = new UserRepository(em);
        RoleRepository roleRepository = new RoleRepository(em);
        GroupRepository groupRepository = new GroupRepository(em);
        return new CustomUserStorageProvider(session, model, userRepository, roleRepository, groupRepository);
    }

//    @Override
//    public CustomUserStorageProvider create(KeycloakSession session, ComponentModel model) {
//        String dbConnectionName = model.getConfig().getFirst("db:connectionName");
//        EntityManager entityManager = entityManagerFactories.get(dbConnectionName);
//
//        if(entityManager == null) {
//            MultivaluedHashMap<String, String> config = model.getConfig();
//
//            Properties properties = new Properties();
//            properties.put("hibernate.connection.driver_class", "com.mysql.cj.jdbc.Driver");
//            properties.put("hibernate.connection.url",
//                    String.format("jdbc:mysql://%s:%s/%s",
//                            config.getFirst(DB_HOST_KEY),
//                            config.getFirst(DB_PORT_KEY),
//                            config.getFirst(DB_DATABASE_KEY)));
//            properties.put("hibernate.connection.username", config.getFirst(DB_USERNAME_KEY));
//            properties.put("hibernate.connection.password", config.getFirst(DB_PASSWORD_KEY));
//            properties.put("hibernate.show-sql", "true");
//            properties.put("hibernate.archive.autodetection", "class, hbm");
//            properties.put("hibernate.hbm2ddl.auto", "update");
//            properties.put("hibernate.connection.autocommit", "true");
//
//
//            SessionFactory sessionFactory = new Configuration().addProperties(properties).buildSessionFactory();
//            entityManager = sessionFactory.openSession();
//            entityManagerFactories.put(dbConnectionName, entityManager);
//        }
//        UserRepository userRepository = new UserRepository(entityManager);
//        return new CustomUserStorageProvider(session, userRepository);
//    }

//    @Override
//    public void onUpdate(KeycloakSession session, RealmModel realm, ComponentModel oldModel, ComponentModel newModel) {
//        String oldCnName = oldModel.getConfig().getFirst(DB_CONNECTION_NAME_KEY);
//        entityManagerFactories.remove(oldCnName);
//        onCreate(session, realm, newModel);
//    }
//
//    @Override
//    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel config) throws ComponentValidationException {
//        MultivaluedHashMap<String, String> configMap = config.getConfig();
//        if(configMap.getFirst(DB_CONNECTION_NAME_KEY) == null || configMap.getFirst(DB_CONNECTION_NAME_KEY).isEmpty()) {
//            throw new ComponentValidationException("Connection name empty.");
//        }
//        if(configMap.getFirst(DB_HOST_KEY) == null || configMap.getFirst(DB_HOST_KEY).isEmpty()) {
//            throw new ComponentValidationException("Database host empty.");
//        }
//        if(!isNumber(configMap.getFirst(DB_PORT_KEY)) || Long.parseLong(configMap.getFirst(DB_PORT_KEY)) > PORT_LIMIT) {
//            throw new ComponentValidationException("Invalid port. (Empty or NaN)");
//        }
//        if(configMap.getFirst(DB_DATABASE_KEY) == null || configMap.getFirst(DB_DATABASE_KEY).isEmpty()) {
//            throw new ComponentValidationException("Database name empty.");
//        }
//        if(configMap.getFirst(DB_USERNAME_KEY) == null || configMap.getFirst(DB_USERNAME_KEY).isEmpty()) {
//            throw new ComponentValidationException("Database username empty.");
//        }
//        if(configMap.getFirst(DB_PASSWORD_KEY) == null || configMap.getFirst(DB_PASSWORD_KEY).isEmpty()) {
//            throw new ComponentValidationException("Database password empty.");
//        }
//    }

    private PersistenceUnitInfo getPersistenceUnitInfo(String name) {
        return new PersistenceUnitInfo() {
            @Override
            public String getPersistenceUnitName() {
                return name;
            }

            @Override
            public String getPersistenceProviderClassName() {
                return "org.hibernate.jpa.HibernatePersistenceProvider";
            }

            @Override
            public PersistenceUnitTransactionType getTransactionType() {
                return PersistenceUnitTransactionType.RESOURCE_LOCAL;
            }

            @Override
            public DataSource getJtaDataSource() {
                return null;
            }

            @Override
            public DataSource getNonJtaDataSource() {
                return null;
            }

            @Override
            public List<String> getMappingFileNames() {
                return Collections.emptyList();
            }

            @Override
            public List<URL> getJarFileUrls() {
                try {
                    return Collections.list(this.getClass()
                            .getClassLoader()
                            .getResources(""));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }

            @Override
            public URL getPersistenceUnitRootUrl() {
                return null;
            }

            @Override
            public List<String> getManagedClassNames() {
                List<String> managedClasses = new LinkedList<>();
                managedClasses.add(UserEntity.class.getName());
                return managedClasses;
            }

            @Override
            public boolean excludeUnlistedClasses() {
                return false;
            }

            @Override
            public SharedCacheMode getSharedCacheMode() {
                return SharedCacheMode.UNSPECIFIED;
            }

            @Override
            public ValidationMode getValidationMode() {
                return ValidationMode.AUTO;
            }

            @Override
            public Properties getProperties() {
                return new Properties();
            }

            @Override
            public String getPersistenceXMLSchemaVersion() {
                return "2.1";
            }

            @Override
            public ClassLoader getClassLoader() {
                return null;
//                return Thread.currentThread().getContextClassLoader();
            }

            @Override
            public void addTransformer(ClassTransformer transformer) {
            }

            @Override
            public ClassLoader getNewTempClassLoader() {
                return null;
            }
        };
    }


    @Override
    public String getId() {
        log.info("CustomUserStorageProviderFactory.getId()");
        return "user-jpa-provider";
    }

    public boolean isNumber(String str) {
        return str.matches("-?\\d+");
    }
}
