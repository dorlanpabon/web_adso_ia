package com.tuyweb.web_adso_ia.util;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.util.HashMap;
import java.util.Map;

public final class JpaUtil {

    private static final String PERSISTENCE_UNIT = "my_persistence_unit";

    private static volatile EntityManagerFactory entityManagerFactory;
    private static volatile RuntimeException initializationException;

    private JpaUtil() {
    }

    public static EntityManager createEntityManager() {
        return getEntityManagerFactory().createEntityManager();
    }

    private static EntityManagerFactory getEntityManagerFactory() {
        if (entityManagerFactory != null) {
            return entityManagerFactory;
        }

        synchronized (JpaUtil.class) {
            if (entityManagerFactory != null) {
                return entityManagerFactory;
            }

            if (initializationException != null) {
                throw initializationException;
            }

            try {
                entityManagerFactory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT, resolveOverrides());
                return entityManagerFactory;
            } catch (Exception ex) {
                initializationException = new IllegalStateException(
                        "No se pudo inicializar JPA. Verifica APP_DB_URL, APP_DB_USER y APP_DB_PASSWORD o el archivo persistence.xml.",
                        ex);
                throw initializationException;
            }
        }
    }

    private static Map<String, Object> resolveOverrides() {
        Map<String, Object> overrides = new HashMap<>();

        putIfPresent(overrides, "jakarta.persistence.jdbc.url",
                getConfig("APP_DB_URL", "DB_URL", "JPA_DB_URL"));
        putIfPresent(overrides, "jakarta.persistence.jdbc.user",
                getConfig("APP_DB_USER", "DB_USER", "JPA_DB_USER"));
        putIfPresent(overrides, "jakarta.persistence.jdbc.password",
                getConfig("APP_DB_PASSWORD", "DB_PASSWORD", "JPA_DB_PASSWORD"));

        return overrides;
    }

    private static String getConfig(String... keys) {
        for (String key : keys) {
            String systemValue = System.getProperty(key);
            if (systemValue != null && !systemValue.isBlank()) {
                return systemValue.trim();
            }

            String envValue = System.getenv(key);
            if (envValue != null && !envValue.isBlank()) {
                return envValue.trim();
            }
        }

        return null;
    }

    private static void putIfPresent(Map<String, Object> overrides, String key, String value) {
        if (value != null && !value.isBlank()) {
            overrides.put(key, value);
        }
    }
}
