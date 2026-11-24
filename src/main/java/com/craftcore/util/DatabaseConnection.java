package com.craftcore.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Utilidad para obtener conexiones JDBC directas mediante DriverManager (RA2).
 */
public final class DatabaseConnection {

    private static final String DEFAULT_URL = "jdbc:postgresql://localhost:5432/craftcore_db";
    private static final String DEFAULT_USER = "postgres";
    private static final String DEFAULT_PASSWORD = "1234";

    private DatabaseConnection() {
    }

    /**
     * Abre una nueva conexión JDBC hacia la base de datos configurada.
     */
    public static Connection getConnection() throws SQLException {
        String url = getConfig("CRAFTCORE_DB_URL", DEFAULT_URL);
        String user = getConfig("CRAFTCORE_DB_USER", DEFAULT_USER);
        String password = getConfig("CRAFTCORE_DB_PASSWORD", DEFAULT_PASSWORD);
        return DriverManager.getConnection(url, user, password);
    }

    /**
     * Verifica si la conexión JDBC es válida.
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && conn.isValid(3);
        } catch (SQLException ex) {
            return false;
        }
    }

    private static String getConfig(String envVar, String defaultValue) {
        String value = System.getenv(envVar);
        return (value != null && !value.isBlank()) ? value : defaultValue;
    }
}
