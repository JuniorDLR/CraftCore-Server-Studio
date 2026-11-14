-- ========================================================
-- Script DDL: Base de Datos CraftCore Server Studio
-- Compatible con PostgreSQL y SQL Server
-- ========================================================

-- 1. Tabla: administradores (Operadores del sistema con acceso al panel)
CREATE TABLE IF NOT EXISTS administradores (
    id_admin SERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nombre_completo VARCHAR(100) NOT NULL,
    rol VARCHAR(20) NOT NULL DEFAULT 'MODERADOR', -- 'SUPERADMIN', 'MODERADOR'
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Tabla: jugadores (Perfiles conocidos, validados vía API Mojang o detectados)
CREATE TABLE IF NOT EXISTS jugadores (
    id_jugador SERIAL PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE,
    nickname VARCHAR(16) NOT NULL,
    skin_url VARCHAR(255),
    es_op BOOLEAN NOT NULL DEFAULT FALSE,
    en_whitelist BOOLEAN NOT NULL DEFAULT FALSE,
    primera_conexion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ultima_conexion TIMESTAMP
);

-- 3. Tabla: sanciones (Historial de moderación: BAN, KICK, MUTE, WARN)
-- Relación ManyToOne con jugadores y administradores
CREATE TABLE IF NOT EXISTS sanciones (
    id_sancion SERIAL PRIMARY KEY,
    id_jugador INT NOT NULL REFERENCES jugadores(id_jugador) ON DELETE CASCADE,
    id_admin INT NOT NULL REFERENCES administradores(id_admin) ON DELETE RESTRICT,
    tipo VARCHAR(20) NOT NULL, -- 'BAN', 'KICK', 'WARN', 'MUTE'
    motivo VARCHAR(255) NOT NULL,
    fecha_inicio TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_fin TIMESTAMP,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO', -- 'ACTIVO', 'REVOCADO', 'EXPIRADO'
    comando_ejecutado VARCHAR(255) NOT NULL
);

-- 4. Tabla: sesiones_servidor (Trazabilidad de inicio y detención del subproceso)
CREATE TABLE IF NOT EXISTS sesiones_servidor (
    id_sesion SERIAL PRIMARY KEY,
    id_admin INT NOT NULL REFERENCES administradores(id_admin) ON DELETE RESTRICT,
    fecha_inicio TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_fin TIMESTAMP,
    jugadores_pico INT NOT NULL DEFAULT 0,
    estado_salida VARCHAR(30) NOT NULL DEFAULT 'EN_EJECUCION' -- 'EN_EJECUCION', 'APAGADO_NORMAL', 'CRASH'
);

-- 5. Tabla: logs_auditoria (Auditoría de acciones y filtrado dinámico JDBC)
CREATE TABLE IF NOT EXISTS logs_auditoria (
    id_log BIGSERIAL PRIMARY KEY,
    id_admin INT REFERENCES administradores(id_admin) ON DELETE SET NULL,
    accion VARCHAR(50) NOT NULL, -- 'INICIO_SRV', 'STOP_SRV', 'EJECUCION_COMANDO', 'WHITELIST_ADD', 'CONFIG_CAMBIO'
    detalle TEXT NOT NULL,
    fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índices para optimizar búsquedas y filtrado de reportes
CREATE INDEX IF NOT EXISTS idx_jugadores_uuid ON jugadores(uuid);
CREATE INDEX IF NOT EXISTS idx_jugadores_nickname ON jugadores(nickname);
CREATE INDEX IF NOT EXISTS idx_sanciones_fecha ON sanciones(fecha_inicio);
CREATE INDEX IF NOT EXISTS idx_logs_fecha ON logs_auditoria(fecha);

-- ========================================================
-- Datos semilla iniciales (Superadministrador por defecto)
-- ========================================================
INSERT INTO administradores (username, password_hash, nombre_completo, rol, activo)
VALUES ('admin', 'admin123', 'Administrador Principal', 'SUPERADMIN', TRUE)
ON CONFLICT (username) DO NOTHING;
