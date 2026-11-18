package com.craftcore.dao.orm;

import com.craftcore.model.entity.Admin;
import com.craftcore.util.HibernateUtil;

import java.util.Optional;

/**
 * DAO Hibernate para la gestión de administradores del sistema.
 */
public class AdminDAO extends BaseHibernateDAO<Admin, Integer> {

    public AdminDAO() {
        super(Admin.class);
    }

    /**
     * Busca un administrador por su nombre de usuario.
     */
    public Optional<Admin> findByUsername(String username) {
        return HibernateUtil.executeQuery(session ->
                session.createQuery("FROM Admin a WHERE a.username = :username", Admin.class)
                        .setParameter("username", username)
                        .uniqueResultOptional()
        );
    }
}
