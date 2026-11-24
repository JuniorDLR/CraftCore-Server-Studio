package com.craftcore.util;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Utilidad Singleton para administrar la SessionFactory de Hibernate (RA4).
 */
public final class HibernateUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(HibernateUtil.class);
    private static volatile SessionFactory sessionFactory;
    private static StandardServiceRegistry registry;

    private HibernateUtil() {
    }

    /**
     * Obtiene la instancia única de SessionFactory, inicializándola de forma perezosa.
     */
    public static SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            synchronized (HibernateUtil.class) {
                if (sessionFactory == null) {
                    sessionFactory = buildSessionFactory();
                }
            }
        }
        return sessionFactory;
    }

    private static SessionFactory buildSessionFactory() {
        try {
            StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder()
                    .configure("hibernate.cfg.xml");

            applyEnvOverride(builder, "hibernate.connection.url", "CRAFTCORE_DB_URL");
            applyEnvOverride(builder, "hibernate.connection.username", "CRAFTCORE_DB_USER");
            applyEnvOverride(builder, "hibernate.connection.password", "CRAFTCORE_DB_PASSWORD");

            registry = builder.build();
            MetadataSources sources = new MetadataSources(registry);
            Metadata metadata = sources.getMetadataBuilder().build();
            return metadata.getSessionFactoryBuilder().build();
        } catch (Exception ex) {
            LOGGER.error("Error al inicializar Hibernate SessionFactory", ex);
            if (registry != null) {
                StandardServiceRegistryBuilder.destroy(registry);
            }
            throw new ExceptionInInitializerError(ex);
        }
    }

    private static void applyEnvOverride(StandardServiceRegistryBuilder builder, String setting, String envVar) {
        String value = System.getenv(envVar);
        if (value != null && !value.isBlank()) {
            builder.applySetting(setting, value);
        }
    }

    /**
     * Ejecuta una operación de escritura sin valor de retorno dentro de una transacción con rollback automático.
     */
    public static void runInTransaction(Consumer<Session> action) {
        executeInTransaction(session -> {
            action.accept(session);
            return null;
        });
    }

    /**
     * Ejecuta una operación con retorno dentro de una transacción con rollback automático ante errores.
     */
    public static <R> R executeInTransaction(Function<Session, R> action) {
        Transaction tx = null;
        try (Session session = getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            R result = action.apply(session);
            tx.commit();
            return result;
        } catch (RuntimeException ex) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw ex;
        }
    }

    /**
     * Ejecuta una consulta de solo lectura usando una sesión de Hibernate.
     */
    public static <R> R executeQuery(Function<Session, R> query) {
        try (Session session = getSessionFactory().openSession()) {
            return query.apply(session);
        }
    }

    /**
     * Cierra la SessionFactory y libera el registro al finalizar la aplicación.
     */
    public static void shutdown() {
        if (sessionFactory != null && !sessionFactory.isClosed()) {
            sessionFactory.close();
        }
        if (registry != null) {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }
}
