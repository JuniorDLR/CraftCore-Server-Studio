package com.craftcore.dao.jdbc;

import com.craftcore.model.entity.Admin;
import com.craftcore.model.entity.AuditLog;
import com.craftcore.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO implementado con JDBC puro (PreparedStatement y ResultSet) para auditoría dinámica (RA2).
 */
public class AuditLogJdbcDAO {

    private static final String BASE_SELECT =
            "SELECT l.id_log, l.id_admin, a.username, l.accion, l.detalle, l.fecha " +
            "FROM logs_auditoria l LEFT JOIN administradores a ON l.id_admin = a.id_admin ";

    /**
     * Inserta un nuevo registro de auditoría usando PreparedStatement.
     */
    public long insert(Integer idAdmin, String accion, String detalle) throws SQLException {
        String sql = "INSERT INTO logs_auditoria (id_admin, accion, detalle) VALUES (?, ?, ?) RETURNING id_log";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (idAdmin != null) {
                ps.setInt(1, idAdmin);
            } else {
                ps.setNull(1, Types.INTEGER);
            }
            ps.setString(2, accion);
            ps.setString(3, detalle);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return -1L;
    }

    /**
     * Lista todos los registros de auditoría ordenados del más reciente al más antiguo.
     */
    public List<AuditLog> findAll() throws SQLException {
        return findByFilters(null, null, null);
    }

    /**
     * Consulta registros de auditoría aplicando filtros dinámicos por rango de fechas y tipo de acción.
     */
    public List<AuditLog> findByFilters(LocalDate desde, LocalDate hasta, String accion) throws SQLException {
        StringBuilder sql = new StringBuilder(BASE_SELECT).append("WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (desde != null) {
            sql.append("AND l.fecha >= ? ");
            params.add(Timestamp.valueOf(desde.atStartOfDay()));
        }
        if (hasta != null) {
            sql.append("AND l.fecha <= ? ");
            params.add(Timestamp.valueOf(LocalDateTime.of(hasta, LocalTime.MAX)));
        }
        if (accion != null && !accion.isBlank() && !"TODOS".equalsIgnoreCase(accion)) {
            sql.append("AND l.accion = ? ");
            params.add(accion);
        }
        sql.append("ORDER BY l.fecha DESC");

        List<AuditLog> logs = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapRow(rs));
                }
            }
        }
        return logs;
    }

    /**
     * Elimina un registro de auditoría por su identificador.
     */
    public boolean deleteById(long idLog) throws SQLException {
        String sql = "DELETE FROM logs_auditoria WHERE id_log = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idLog);
            return ps.executeUpdate() > 0;
        }
    }

    private AuditLog mapRow(ResultSet rs) throws SQLException {
        AuditLog log = new AuditLog();
        log.setIdLog(rs.getLong("id_log"));

        int idAdmin = rs.getInt("id_admin");
        if (!rs.wasNull()) {
            Admin admin = new Admin();
            admin.setIdAdmin(idAdmin);
            admin.setUsername(rs.getString("username"));
            log.setAdmin(admin);
        }

        log.setAccion(rs.getString("accion"));
        log.setDetalle(rs.getString("detalle"));

        Timestamp fecha = rs.getTimestamp("fecha");
        if (fecha != null) {
            log.setFecha(fecha.toLocalDateTime());
        }
        return log;
    }
}
