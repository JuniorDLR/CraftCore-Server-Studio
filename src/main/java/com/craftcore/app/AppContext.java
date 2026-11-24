package com.craftcore.app;

import com.craftcore.dao.orm.AdminDAO;
import com.craftcore.model.dto.OnlinePlayer;
import com.craftcore.model.entity.Admin;
import com.craftcore.service.ConfigurationFileService;
import com.craftcore.service.MinecraftProcessService;
import com.craftcore.service.ModerationService;
import com.craftcore.service.MojangApiService;
import com.craftcore.service.ReportGeneratorService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Contenedor compartido de servicios y estado de sesión para las 5 vistas JavaFX.
 */
public final class AppContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppContext.class);
    private static final AppContext INSTANCE = new AppContext();

    private final MinecraftProcessService processService = new MinecraftProcessService();
    private final MojangApiService mojangApiService = new MojangApiService();
    private final ConfigurationFileService configFileService = new ConfigurationFileService();
    private final ModerationService moderationService = new ModerationService(processService);
    private final ReportGeneratorService reportGeneratorService = new ReportGeneratorService();
    private final ObservableList<OnlinePlayer> onlinePlayers = FXCollections.observableArrayList();

    private volatile Admin currentAdmin;

    private AppContext() {
        registerProcessPlayerListeners();
    }

    public static AppContext getInstance() {
        return INSTANCE;
    }

    private void registerProcessPlayerListeners() {
        processService.addPlayerJoinListener(nickname -> {
            boolean alreadyPresent = onlinePlayers.stream()
                    .anyMatch(p -> p.getNickname().equalsIgnoreCase(nickname));
            if (!alreadyPresent) {
                String offlineUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + nickname).getBytes()).toString();
                String avatarUrl = "https://mc-heads.net/avatar/" + nickname + "/32";
                onlinePlayers.add(new OnlinePlayer(nickname, offlineUuid, 25, avatarUrl));
            }
        });

        processService.addPlayerLeaveListener(nickname ->
                onlinePlayers.removeIf(p -> p.getNickname().equalsIgnoreCase(nickname))
        );

        processService.addStateListener(state -> {
            if (state == MinecraftProcessService.ServerState.STOPPED
                    || state == MinecraftProcessService.ServerState.CRASHED) {
                onlinePlayers.clear();
            }
        });
    }

    public MinecraftProcessService getProcessService() {
        return processService;
    }

    public MojangApiService getMojangApiService() {
        return mojangApiService;
    }

    public ConfigurationFileService getConfigFileService() {
        return configFileService;
    }

    public ModerationService getModerationService() {
        return moderationService;
    }

    public ReportGeneratorService getReportGeneratorService() {
        return reportGeneratorService;
    }

    public ObservableList<OnlinePlayer> getOnlinePlayers() {
        return onlinePlayers;
    }

    public Admin getCurrentAdmin() {
        if (currentAdmin == null) {
            try {
                currentAdmin = new AdminDAO().findByUsername("admin").orElse(null);
            } catch (Exception ex) {
                LOGGER.warn("No se pudo cargar el administrador por defecto desde PostgreSQL: {}", ex.getMessage());
            }
        }
        return currentAdmin;
    }
}
