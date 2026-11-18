package com.craftcore.controller;

import com.craftcore.app.AppContext;
import com.craftcore.app.MainApp;
import com.craftcore.service.ReportGeneratorService.ExportFormat;
import com.craftcore.service.ReportGeneratorService.ReportType;
import com.craftcore.util.AlertHelper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Controlador [P5]: Exportador JasperReports y editor de server.properties.
 */
public class ReportsConfigController {

    @FXML private ComboBox<ReportType> cmbReportType;
    @FXML private DatePicker dpReportDesde;
    @FXML private DatePicker dpReportHasta;
    @FXML private RadioButton rbPdf;
    @FXML private RadioButton rbExcel;

    @FXML private TextField txtMotd;
    @FXML private TextField txtPort;
    @FXML private TextField txtMaxPlayers;
    @FXML private ComboBox<String> cmbGamemode;
    @FXML private ComboBox<String> cmbDifficulty;
    @FXML private CheckBox chkWhitelist;
    @FXML private CheckBox chkPvp;
    @FXML private CheckBox chkOnlineMode;

    private final AppContext context = AppContext.getInstance();
    private Properties currentProperties = new Properties();

    @FXML
    public void initialize() {
        cmbReportType.setItems(FXCollections.observableArrayList(ReportType.values()));
        cmbReportType.getSelectionModel().selectFirst();

        cmbGamemode.setItems(FXCollections.observableArrayList("survival", "creative", "adventure", "spectator"));
        cmbDifficulty.setItems(FXCollections.observableArrayList("peaceful", "easy", "normal", "hard"));

        loadServerProperties();
    }

    @FXML
    private void handleExportReport() {
        ReportType type = cmbReportType.getValue();
        ExportFormat format = rbPdf.isSelected() ? ExportFormat.PDF : ExportFormat.EXCEL;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Guardar Reporte");
        String ext = format == ExportFormat.PDF ? "*.pdf" : "*.xlsx";
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(format.name(), ext));
        chooser.setInitialFileName(type.name().toLowerCase() + (format == ExportFormat.PDF ? ".pdf" : ".xlsx"));

        File file = chooser.showSaveDialog(MainApp.getPrimaryStage());
        if (file == null) {
            return;
        }

        try {
            context.getReportGeneratorService().generateReport(
                    type, format, dpReportDesde.getValue(), dpReportHasta.getValue(), file);
            AlertHelper.showInfo("Reporte exportado", "Archivo generado en:\n" + file.getAbsolutePath());
        } catch (Exception ex) {
            AlertHelper.showError("Exportación de Reporte", ex.getMessage());
        }
    }

    @FXML
    public void loadServerProperties() {
        try {
            Path serverDir = context.getProcessService().getServerDirectory();
            currentProperties = context.getConfigFileService().loadServerProperties(serverDir);

            txtMotd.setText(currentProperties.getProperty("motd", "CraftCore Minecraft Server"));
            txtPort.setText(currentProperties.getProperty("server-port", "25565"));
            txtMaxPlayers.setText(currentProperties.getProperty("max-players", "20"));
            cmbGamemode.setValue(currentProperties.getProperty("gamemode", "survival"));
            cmbDifficulty.setValue(currentProperties.getProperty("difficulty", "normal"));
            chkWhitelist.setSelected(Boolean.parseBoolean(currentProperties.getProperty("white-list", "false")));
            chkPvp.setSelected(Boolean.parseBoolean(currentProperties.getProperty("pvp", "true")));
            chkOnlineMode.setSelected(Boolean.parseBoolean(currentProperties.getProperty("online-mode", "true")));
        } catch (Exception ex) {
            AlertHelper.showError("Error cargando server.properties", ex.getMessage());
        }
    }

    @FXML
    private void handleSaveProperties() {
        int port;
        int maxPlayers;
        try {
            port = Integer.parseInt(txtPort.getText().trim());
            maxPlayers = Integer.parseInt(txtMaxPlayers.getText().trim());
            if (port < 1 || port > 65535 || maxPlayers < 1) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            AlertHelper.showWarning("Validación", "Puerto (1-65535) y Máx. Jugadores (>0) deben ser números válidos.");
            return;
        }

        currentProperties.setProperty("motd", txtMotd.getText() == null ? "" : txtMotd.getText().trim());
        currentProperties.setProperty("server-port", String.valueOf(port));
        currentProperties.setProperty("max-players", String.valueOf(maxPlayers));
        currentProperties.setProperty("gamemode", cmbGamemode.getValue());
        currentProperties.setProperty("difficulty", cmbDifficulty.getValue());
        currentProperties.setProperty("white-list", String.valueOf(chkWhitelist.isSelected()));
        currentProperties.setProperty("pvp", String.valueOf(chkPvp.isSelected()));
        currentProperties.setProperty("online-mode", String.valueOf(chkOnlineMode.isSelected()));

        try {
            Path serverDir = context.getProcessService().getServerDirectory();
            context.getConfigFileService().saveServerProperties(serverDir, currentProperties);
            context.getModerationService().logAudit(context.getCurrentAdmin(), "CONFIG_CAMBIO",
                    "Actualizado server.properties (puerto=" + port + ", maxPlayers=" + maxPlayers + ")");
            AlertHelper.showInfo("Configuración guardada", "server.properties guardado correctamente.");
        } catch (Exception ex) {
            AlertHelper.showError("Error guardando server.properties", ex.getMessage());
        }
    }
}
