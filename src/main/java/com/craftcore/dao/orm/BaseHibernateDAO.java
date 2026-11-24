package com.craftcore.dao.orm;

import com.craftcore.util.HibernateUtil;

import java.util.List;
import java.util.Optional;

/**
 * DAO genérico base con operaciones CRUD estándar sobre Hibernate ORM (RA4).
 */
public abstract class BaseHibernateDAO<T, ID> {

    private final Class<T> entityClass;

    protected BaseHibernateDAO(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    /**
     * Persiste una nueva entidad en la base de datos.
     */
    public T save(T entity) {
        HibernateUtil.runInTransaction(session -> session.persist(entity));
        return entity;
    }

    /**
     * Actualiza una entidad existente usando merge.
     */
    public T update(T entity) {
        return HibernateUtil.executeInTransaction(session -> session.merge(entity));
    }

    /**
     * Busca una entidad por su clave primaria.
     */
    public Optional<T> findById(ID id) {
        return HibernateUtil.executeQuery(session ->
                Optional.ofNullable(session.find(entityClass, id))
        );
    }

    /**
     * Lista todas las entidades registradas de este tipo.
     */
    public List<T> findAll() {
        String hql = "FROM " + entityClass.getSimpleName();
        return HibernateUtil.executeQuery(session ->
                session.createQuery(hql, entityClass).list()
        );
    }

    /**
     * Elimina una entidad por su identificador si existe.
     */
    public boolean deleteById(ID id) {
        return HibernateUtil.executeInTransaction(session -> {
            T entity = session.find(entityClass, id);
            if (entity == null) {
                return false;
            }
            session.remove(entity);
            return true;
        });
    }
}
