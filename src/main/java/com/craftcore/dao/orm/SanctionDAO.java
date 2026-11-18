package com.craftcore.dao.orm;

import com.craftcore.model.entity.Sanction;
import com.craftcore.util.HibernateUtil;
import org.hibernate.query.Query;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * DAO Hibernate para el historial y gestión de sanciones (RA4).
 */
public class SanctionDAO extends BaseHibernateDAO<Sanction, Integer> {

    public SanctionDAO() {
        super(Sanction.class);
    }

    /**
     * Lista todas las sanciones cargando jugador y administrador asociados para evitar LazyInitializationException.
     */
    public List<Sanction> findAllWithDetails() {
        return HibernateUtil.executeQuery(session ->
                session.createQuery(
                        "SELECT s FROM Sanction s JOIN FETCH s.jugador JOIN FETCH s.admin ORDER BY s.fechaInicio DESC",
                        Sanction.class
                ).list()
        );
    }

    /**
     * Obtiene las sanciones de un jugador específico.
     */
    public List<Sanction> findByPlayerId(Integer idJugador) {
        return HibernateUtil.executeQuery(session ->
                session.createQuery(
                        "SELECT s FROM Sanction s JOIN FETCH s.jugador j JOIN FETCH s.admin WHERE j.idJugador = :idJugador ORDER BY s.fechaInicio DESC",
                        Sanction.class
                ).setParameter("idJugador", idJugador).list()
        );
    }

    /**
     * Filtra sanciones por rango de fechas y tipo de sanción.
     */
    public List<Sanction> findByFilters(LocalDate desde, LocalDate hasta, String tipo) {
        return HibernateUtil.executeQuery(session -> {
            StringBuilder hql = new StringBuilder(
                    "SELECT s FROM Sanction s JOIN FETCH s.jugador JOIN FETCH s.admin WHERE 1=1"
            );

            if (desde != null) {
                hql.append(" AND s.fechaInicio >= :desde");
            }
            if (hasta != null) {
                hql.append(" AND s.fechaInicio <= :hasta");
            }
            if (tipo != null && !tipo.isBlank() && !"TODOS".equalsIgnoreCase(tipo)) {
                hql.append(" AND s.tipo = :tipo");
            }
            hql.append(" ORDER BY s.fechaInicio DESC");

            Query<Sanction> query = session.createQuery(hql.toString(), Sanction.class);
            if (desde != null) {
                query.setParameter("desde", desde.atStartOfDay());
            }
            if (hasta != null) {
                query.setParameter("hasta", LocalDateTime.of(hasta, LocalTime.MAX));
            }
            if (tipo != null && !tipo.isBlank() && !"TODOS".equalsIgnoreCase(tipo)) {
                query.setParameter("tipo", tipo);
            }
            return query.list();
        });
    }
}
