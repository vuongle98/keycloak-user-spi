package org.vuong.keycloak.spi.config;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JpaProvider {

    private static final Logger log = LoggerFactory.getLogger(JpaProvider.class);
    private static SessionFactory sessionFactory = null;

    public static Session getEntityManager(String jdbcUrl, String dbUsername, String dbPassword, String dialect) {
        log.debug("Creating Hibernate Session with dynamic parameters: URL={}, User={}, Dialect={}", jdbcUrl, dbUsername, dialect);
        Configuration configuration = new Configuration();

        // Set Hibernate connection properties from dynamic input
        configuration.setProperty("hibernate.connection.driver_class", "org.postgresql.Driver"); // Assuming PostgreSQL
        configuration.setProperty("hibernate.connection.url", jdbcUrl);
        configuration.setProperty("hibernate.connection.username", dbUsername);
        configuration.setProperty("hibernate.connection.password", dbPassword);
        configuration.setProperty("hibernate.dialect", dialect);

        // Echo all executed SQL to stdout (configurable)
        configuration.setProperty("hibernate.show_sql", "false"); // You can make this a config property if needed
        configuration.setProperty("hibernate.format_sql", "true"); // You can make this a config property if needed

        // Schema validation (configurable)
        configuration.setProperty("hibernate.hbm2ddl.auto", "validate"); // You can make this a config property

        // JDBC connection pool (configurable)
        configuration.setProperty("hibernate.connection.pool_size", "5"); // You can make this a config property

        // If using annotations, you would register the classes directly:
         configuration.addAnnotatedClass(org.vuong.keycloak.spi.entity.UserEntity.class);
         configuration.addAnnotatedClass(org.vuong.keycloak.spi.entity.UserProfile.class);
         configuration.addAnnotatedClass(org.vuong.keycloak.spi.entity.Role.class);
         configuration.addAnnotatedClass(org.vuong.keycloak.spi.entity.Permission.class);
         configuration.addAnnotatedClass(org.vuong.keycloak.spi.entity.Group.class);

        try {
            if (sessionFactory == null) {
                sessionFactory = configuration.buildSessionFactory();
            }

            return sessionFactory.openSession();
        } catch (Throwable ex) {
            log.error("Failed to create Hibernate SessionFactory with dynamic parameters: {}", ex.getMessage(), ex);
            throw new ExceptionInInitializerError("Failed to create Hibernate SessionFactory with dynamic parameters: " + ex);
        }
    }

    public static Session getSession() {
        if (sessionFactory == null) {
            try {
                sessionFactory = new Configuration()
                        .configure("hibernate.cfg.xml")
                        .buildSessionFactory();
            } catch (Throwable ex) {
                log.error("Initial SessionFactory creation failed: {}", ex.getMessage(), ex);
                throw new ExceptionInInitializerError("Initial SessionFactory creation failed: " + ex);
            }
        }
        return sessionFactory.openSession();
    }

    public static void closeSessionFactory() {
        if (sessionFactory != null && !sessionFactory.isClosed()) {
            log.info("Closing Hibernate SessionFactory.");
            sessionFactory.close();
        }
    }
}