package com.craftcore.service;

import com.craftcore.dao.jdbc.AuditLogJdbcDAO;
import com.craftcore.dao.orm.AdminDAO;
import com.craftcore.dao.orm.PlayerDAO;
import com.craftcore.dao.orm.SanctionDAO;
import com.craftcore.model.entity.Admin;
import com.craftcore.model.entity.Player;
import com.craftcore.model.entity.Sanction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Servicio puente de moderación en 1 clic:
 * Inyecta el comando en la consola del servidor y persiste la sanción y auditoría en PostgreSQL.
 */
public class ModerationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModerationService.class);

    private final MinecraftProcessService processService;
    private final PlayerDAO playerDAO;
    private final SanctionDAO sanctionDAO;
    private final AdminDAO adminDAO;
    private final AuditLogJdbcDAO auditLogDAO;

    public ModerationService(MinecraftProcessService processService) {
        this.processService = processService;
        this.playerDAO = new PlayerDAO();
        this.sanctionDAO = new SanctionDAO();
        this.adminDAO = new AdminDAO();
        this.auditLogDAO = new AuditLogJdbcDAO();
    }

    /**
     * Aplica una sanción (BAN, KICK, WARN, MUTE) al jugador, envía el comando y registra en BD.
     */
    public Sanction applySanction(String nickname, String uuid, String tipo, String motivo, Admin operator) {
        String command = buildSanctionCommand(tipo, nickname, motivo);
        processService.sendCommand(command);

        Player player = findOrCreatePlayer(nickname, uuid);
        Admin admin = resolveOperator(operator);

        Sanction sanction = new Sanction(player, admin, tipo.toUpperCase(), motivo, "/" + command);
        sanction.setEstado("KICK".equalsIgnoreCase(tipo) ? "EXPIRADO" : "ACTIVO");
        sanctionDAO.save(sanction);

        logAudit(admin, "EJECUCION_COMANDO", "Sanción " + tipo + " a " + nickname + ": " + motivo);
        return sanction;
    }

    /**
     * Otorga o revoca operador (/op o /deop) y registra el evento en auditoría.
     */
    public void toggleOperator(String nickname, String uuid, boolean grantOp, Admin operator) {
        String command = (grantOp ? "op " : "deop ") + nickname;
        processService.sendCommand(command);

        Player player = findOrCreatePlayer(nickname, uuid);
        player.setEsOp(grantOp);
        playerDAO.update(player);

        Admin admin = resolveOperator(operator);
        logAudit(admin, "EJECUCION_COMANDO", "Comando /" + command);
    }

    /**
     * Cambia el modo de juego de un jugador (/gamemode) y registra el evento en auditoría.
     */
    public void changeGamemode(String nickname, String gamemode, Admin operator) {
        String command = "gamemode " + gamemode + " " + nickname;
        processService.sendCommand(command);

        Admin admin = resolveOperator(operator);
        logAudit(admin, "EJECUCION_COMANDO", "Comando /" + command);
    }

    public void logAudit(Admin operator, String accion, String detalle) {
        try {
            Integer idAdmin = operator != null ? operator.getIdAdmin() : null;
            auditLogDAO.insert(idAdmin, accion, detalle);
        } catch (SQLException ex) {
            LOGGER.error("No se pudo registrar el log de auditoría JDBC", ex);
        }
    }

    private Player findOrCreatePlayer(String nickname, String uuid) {
        return playerDAO.findByNickname(nickname).orElseGet(() -> {
            String resolvedUuid = (uuid != null && !uuid.isBlank())
                    ? uuid
                    : UUID.nameUUIDFromBytes(("OfflinePlayer:" + nickname).getBytes()).toString();
            Player p = new Player(resolvedUuid, nickname);
            p.setUltimaConexion(LocalDateTime.now());
            return playerDAO.save(p);
        });
    }

    private Admin resolveOperator(Admin operator) {
        if (operator != null && operator.getIdAdmin() != null) {
            return operator;
        }
        return adminDAO.findByUsername("admin")
                .orElseThrow(() -> new IllegalStateException("No existe un administrador base en la base de datos."));
    }

    private String buildSanctionCommand(String tipo, String nickname, String motivo) {
        String cleanReason = (motivo == null || motivo.isBlank()) ? "Moderación administrativa" : motivo.trim();
        return switch (tipo.toUpperCase()) {
            case "BAN" -> "ban " + nickname + " " + cleanReason;
            case "KICK" -> "kick " + nickname + " " + cleanReason;
            case "MUTE" -> "mute " + nickname + " " + cleanReason;
            case "WARN" -> "tellraw " + nickname + " {\"text\":\"[ADVERTENCIA] " + cleanReason + "\",\"color\":\"red\"}";
            default -> "say [" + tipo + "] " + nickname + ": " + cleanReason;
        };
    }
}
