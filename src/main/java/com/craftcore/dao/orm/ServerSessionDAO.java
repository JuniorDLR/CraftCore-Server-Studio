package com.craftcore.dao.orm;

import com.craftcore.model.entity.ServerSession;
import com.craftcore.util.HibernateUtil;

import java.util.Optional;

/**
 * DAO Hibernate para el control de sesiones de ejecución del servidor.
 */
public class ServerSessionDAO extends BaseHibernateDAO<ServerSession, Integer> {

    public ServerSessionDAO() {
        super(ServerSession.class);
    }

    /**
     * Obtiene la última sesión que aún figura en ejecución, si existe.
     */
    public Optional<ServerSession> findActiveSession() {
        return HibernateUtil.executeQuery(session ->
                session.createQuery(
                        "FROM ServerSession s WHERE s.estadoSalida = 'EN_EJECUCION' ORDER BY s.fechaInicio DESC",
                        ServerSession.class
                ).setMaxResults(1).uniqueResultOptional()
        );
    }
}
