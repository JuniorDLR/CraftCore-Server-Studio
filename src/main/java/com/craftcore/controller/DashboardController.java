package com.craftcore.controller;

import com.craftcore.app.AppContext;
import com.craftcore.app.MainApp;
import com.craftcore.dao.orm.ServerSessionDAO;
import com.craftcore.model.entity.Admin;
import com.craftcore.model.entity.ServerSession;
import com.craftcore.service.MinecraftProcessService;
import com.craftcore.service.MinecraftProcessService.ServerState;
import com.craftcore.util.AlertHelper;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Controlador principal [P1]: Consola en vivo, control de ciclo de vida y navegación entre las 5 vistas.
 */
public class DashboardController {

    @FXML private Label lblServerState;
    @FXML private Label lblServerPath;
    @FXML private Button btnStart;
    @FXML private Button btnStop;
    @FXML private Button btnRestart;
    @FXML private Button navDashboard;
    @FXML private Button navModeration;
    @FXML private Button navWhitelist;
    @FXML private Button navAudit;
    @FXML private Button navReports;
    @FXML private StackPane contentArea;
    @FXML private VBox dashboardPanel;
    @FXML private TextArea txtConsole;
    @FXML private TextField txtCommand;

    private final AppContext context = AppContext.getInstance();
    private final MinecraftProcessService processService = context.getProcessService();
    private final ServerSessionDAO sessionDAO = new ServerSessionDAO();
    private ServerSession activeSession;

    @FXML
    public void initialize() {
        updateServerPathLabel();

        processService.addConsoleListener(line -> {
            txtConsole.appendText(line + "\n");
            if (txtConsole.getLength() > 150_000) {
                txtConsole.deleteText(0, 50_000);
            }
        });

        processService.addStateListener(this::onServerStateChanged);
        onServerStateChanged(processService.getCurrentState());
    }

    private static final String NAV_IDLE_STYLE =
            "-fx-background-color: #1e293b; -fx-text-fill: #e2e8f0; -fx-background-radius: 8; -fx-padding: 6 12; -fx-cursor: hand;";
    private static final String NAV_ACTIVE_STYLE =
            "-fx-background-color: #6366f1; -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 6 12; -fx-cursor: hand;";
    private static final String BADGE_BASE_STYLE =
            "-fx-padding: 5 12; -fx-background-radius: 14; -fx-font-weight: bold; -fx-font-size: 12px; ";

    private void onServerStateChanged(ServerState state) {
        switch (state) {
            case STOPPED -> {
                lblServerState.setText("DETENIDO");
                lblServerState.setStyle(BADGE_BASE_STYLE + "-fx-background-color: #334155; -fx-text-fill: #e2e8f0;");
                btnStart.setDisable(false);
                btnStop.setDisable(true);
                btnRestart.setDisable(true);
                closeActiveSession("APAGADO_NORMAL");
            }
            case STARTING -> {
                lblServerState.setText("INICIANDO...");
                lblServerState.setStyle(BADGE_BASE_STYLE + "-fx-background-color: #f59e0b; -fx-text-fill: #0f172a;");
                btnStart.setDisable(true);
                btnStop.setDisable(false);
                btnRestart.setDisable(true);
            }
            case RUNNING -> {
                lblServerState.setText("EN LÍNEA");
                lblServerState.setStyle(BADGE_BASE_STYLE + "-fx-background-color: #10b981; -fx-text-fill: #022c22;");
                btnStart.setDisable(true);
                btnStop.setDisable(false);
                btnRestart.setDisable(false);
            }
            case STOPPING -> {
                lblServerState.setText("DETENIENDO...");
                lblServerState.setStyle(BADGE_BASE_STYLE + "-fx-background-color: #f59e0b; -fx-text-fill: #0f172a;");
                btnStart.setDisable(true);
                btnStop.setDisable(true);
                btnRestart.setDisable(true);
            }
            case CRASHED -> {
                lblServerState.setText("ERROR / CRASH");
                lblServerState.setStyle(BADGE_BASE_STYLE + "-fx-background-color: #ef4444; -fx-text-fill: #ffffff;");
                btnStart.setDisable(false);
                btnStop.setDisable(true);
                btnRestart.setDisable(true);
                closeActiveSession("CRASH");
            }
        }
    }

    @FXML
    private void handleStartServer() {
        try {
            processService.startServer();
            Admin admin = context.getCurrentAdmin();
            if (admin != null) {
                activeSession = sessionDAO.save(new ServerSession(admin));
                context.getModerationService().logAudit(admin, "INICIO_SRV",
                        "Servidor iniciado en " + processService.getServerDirectory());
            }
        } catch (Exception ex) {
            AlertHelper.showError("Error al iniciar servidor", ex.getMessage());
        }
    }

    @FXML
    private void handleStopServer() {
        processService.stopServer();
        context.getModerationService().logAudit(context.getCurrentAdmin(), "STOP_SRV", "Detención manual solicitada.");
    }

    @FXML
    private void handleRestartServer() {
        processService.stopServer();
        new Thread(() -> {
            while (processService.isRunning()) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            Platform.runLater(this::handleStartServer);
        }, "mc-restart-waiter").start();
    }

    @FXML
    private void handleSendCommand() {
        String cmd = txtCommand.getText();
        if (cmd == null || cmd.isBlank()) {
            return;
        }
        processService.sendCommand(cmd);
        txtConsole.appendText("> " + cmd + "\n");
        context.getModerationService().logAudit(context.getCurrentAdmin(), "EJECUCION_COMANDO", cmd);
        txtCommand.clear();
    }

    @FXML
    private void handleClearConsole() {
        txtConsole.clear();
    }

    @FXML
    private void handleQuickMenuCommand(ActionEvent event) {
        if (event.getSource() instanceof MenuItem item && item.getUserData() instanceof String cmd) {
            executeQuickCommand(cmd);
        }
    }

    @FXML
    private void handleQuickSave() {
        executeQuickCommand("save-all");
    }

    @FXML
    private void handleQuickList() {
        executeQuickCommand("list");
    }

    @FXML
    private void handleQuickWhitelistReload() {
        executeQuickCommand("whitelist reload");
    }

    @FXML
    private void handleQuickBroadcast() {
        AlertHelper.showTextInput("Anuncio Global", "Mensaje para todos los jugadores (/say):", "")
                .ifPresent(msg -> executeQuickCommand("say " + msg));
    }

    private void executeQuickCommand(String command) {
        processService.sendCommand(command);
        txtConsole.appendText("> " + command + "\n");
        context.getModerationService().logAudit(context.getCurrentAdmin(), "EJECUCION_COMANDO", command);
    }

    @FXML
    private void handleSelectServerFolder() {
        if (processService.isRunning()) {
            AlertHelper.showWarning("Servidor activo", "Detené el servidor antes de cambiar la carpeta.");
            return;
        }
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Seleccionar carpeta del servidor Minecraft");
        File dir = chooser.showDialog(MainApp.getPrimaryStage());
        if (dir != null) {
            processService.configure(dir.toPath(), processService.getJarFileName(), processService.getMaxMemoryMb());
            updateServerPathLabel();
        }
    }

    @FXML
    private void handleExit() {
        Platform.exit();
    }

    @FXML
    private void handleAbout() {
        AlertHelper.showInfo("Acerca de CraftCore Server Studio",
                "CraftCore Server Studio v1.0.0\n"
                        + "Panel de Administración Visual y Orquestador Concurrente para Minecraft.\n"
                        + "JavaFX 21 + Hibernate 6 + JDBC PostgreSQL + Mojang REST API + JasperReports.");
    }

    @FXML
    private void showDashboard() {
        setActiveNav(navDashboard);
        contentArea.getChildren().setAll(dashboardPanel);
    }

    @FXML
    private void showLiveModeration() {
        loadView("/com/craftcore/fxml/LiveModerationView.fxml", navModeration);
    }

    @FXML
    private void showWhitelist() {
        loadView("/com/craftcore/fxml/WhitelistManagerView.fxml", navWhitelist);
    }

    @FXML
    private void showAuditHistory() {
        loadView("/com/craftcore/fxml/AuditHistoryView.fxml", navAudit);
    }

    @FXML
    private void showReportsConfig() {
        loadView("/com/craftcore/fxml/ReportsConfigView.fxml", navReports);
    }

    private void loadView(String fxmlPath, Button activeBtn) {
        try {
            URL url = getClass().getResource(fxmlPath);
            if (url == null) {
                AlertHelper.showError("Vista no encontrada", "No se encontró el archivo " + fxmlPath);
                return;
            }
            Node view = FXMLLoader.load(url);
            setActiveNav(activeBtn);
            contentArea.getChildren().setAll(view);
        } catch (IOException ex) {
            AlertHelper.showError("Error cargando vista", ex.getMessage());
        }
    }

    private void setActiveNav(Button activeBtn) {
        for (Button btn : List.of(navDashboard, navModeration, navWhitelist, navAudit, navReports)) {
            btn.setStyle(NAV_IDLE_STYLE);
        }
        if (activeBtn != null) {
            activeBtn.setStyle(NAV_ACTIVE_STYLE);
        }
    }

    private void updateServerPathLabel() {
        lblServerPath.setText("Ruta: " + processService.getServerDirectory().resolve(processService.getJarFileName()));
    }

    private void closeActiveSession(String exitState) {
        if (activeSession != null) {
            try {
                activeSession.setFechaFin(LocalDateTime.now());
                activeSession.setEstadoSalida(exitState);
                sessionDAO.update(activeSession);
            } catch (Exception ignored) {
            } finally {
                activeSession = null;
            }
        }
    }
}
