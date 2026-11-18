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
 * Controlador [P5]: Exportador JasperReports y editor completo de server.properties.
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
    @FXML private TextField txtLevelName;
    @FXML private TextField txtLevelSeed;
    @FXML private ComboBox<String> cmbLevelType;
    @FXML private ComboBox<String> cmbGamemode;
    @FXML private ComboBox<String> cmbDifficulty;
    @FXML private TextField txtViewDistance;
    @FXML private TextField txtSimulationDistance;

    @FXML private CheckBox chkWhitelist;
    @FXML private CheckBox chkOnlineMode;
    @FXML private CheckBox chkPvp;
    @FXML private CheckBox chkAllowFlight;
    @FXML private CheckBox chkAllowNether;
    @FXML private CheckBox chkSpawnMonsters;
    @FXML private CheckBox chkSpawnAnimals;
    @FXML private CheckBox chkGenerateStructures;
    @FXML private CheckBox chkHardcore;

    private final AppContext context = AppContext.getInstance();
    private Properties currentProperties = new Properties();

    @FXML
    public void initialize() {
        cmbReportType.setItems(FXCollections.observableArrayList(ReportType.values()));
        cmbReportType.getSelectionModel().selectFirst();

        cmbLevelType.setItems(FXCollections.observableArrayList(
                "minecraft:normal", "minecraft:flat", "minecraft:large_biomes", "minecraft:amplified"));
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
            txtLevelName.setText(currentProperties.getProperty("level-name", "world"));
            txtLevelSeed.setText(currentProperties.getProperty("level-seed", ""));
            cmbLevelType.setValue(currentProperties.getProperty("level-type", "minecraft:normal"));
            cmbGamemode.setValue(currentProperties.getProperty("gamemode", "survival"));
            cmbDifficulty.setValue(currentProperties.getProperty("difficulty", "normal"));
            txtViewDistance.setText(currentProperties.getProperty("view-distance", "10"));
            txtSimulationDistance.setText(currentProperties.getProperty("simulation-distance", "10"));

            chkWhitelist.setSelected(getBool("white-list", false));
            chkOnlineMode.setSelected(getBool("online-mode", true));
            chkPvp.setSelected(getBool("pvp", true));
            chkAllowFlight.setSelected(getBool("allow-flight", false));
            chkAllowNether.setSelected(getBool("allow-nether", true));
            chkSpawnMonsters.setSelected(getBool("spawn-monsters", true));
            chkSpawnAnimals.setSelected(getBool("spawn-animals", true));
            chkGenerateStructures.setSelected(getBool("generate-structures", true));
            chkHardcore.setSelected(getBool("hardcore", false));
        } catch (Exception ex) {
            AlertHelper.showError("Error cargando server.properties", ex.getMessage());
        }
    }

    @FXML
    private void handleSaveProperties() {
        int port;
        int maxPlayers;
        int viewDist;
        int simDist;
        try {
            port = parseRange(txtPort.getText(), 1, 65535);
            maxPlayers = parseRange(txtMaxPlayers.getText(), 1, 1000);
            viewDist = parseRange(txtViewDistance.getText(), 2, 32);
            simDist = parseRange(txtSimulationDistance.getText(), 2, 32);
        } catch (NumberFormatException ex) {
            AlertHelper.showWarning("Validación",
                    "Verificá los valores numéricos:\n- Puerto: 1–65535\n- Máx. Jugadores: 1–1000\n- Visión/Simulación: 2–32 chunks.");
            return;
        }

        String levelName = txtLevelName.getText() == null || txtLevelName.getText().isBlank()
                ? "world"
                : txtLevelName.getText().trim();

        currentProperties.setProperty("motd", txtMotd.getText() == null ? "" : txtMotd.getText().trim());
        currentProperties.setProperty("server-port", String.valueOf(port));
        currentProperties.setProperty("max-players", String.valueOf(maxPlayers));
        currentProperties.setProperty("level-name", levelName);
        currentProperties.setProperty("level-seed", txtLevelSeed.getText() == null ? "" : txtLevelSeed.getText().trim());
        currentProperties.setProperty("level-type", cmbLevelType.getValue());
        currentProperties.setProperty("gamemode", cmbGamemode.getValue());
        currentProperties.setProperty("difficulty", cmbDifficulty.getValue());
        currentProperties.setProperty("view-distance", String.valueOf(viewDist));
        currentProperties.setProperty("simulation-distance", String.valueOf(simDist));

        currentProperties.setProperty("white-list", String.valueOf(chkWhitelist.isSelected()));
        currentProperties.setProperty("online-mode", String.valueOf(chkOnlineMode.isSelected()));
        currentProperties.setProperty("pvp", String.valueOf(chkPvp.isSelected()));
        currentProperties.setProperty("allow-flight", String.valueOf(chkAllowFlight.isSelected()));
        currentProperties.setProperty("allow-nether", String.valueOf(chkAllowNether.isSelected()));
        currentProperties.setProperty("spawn-monsters", String.valueOf(chkSpawnMonsters.isSelected()));
        currentProperties.setProperty("spawn-animals", String.valueOf(chkSpawnAnimals.isSelected()));
        currentProperties.setProperty("generate-structures", String.valueOf(chkGenerateStructures.isSelected()));
        currentProperties.setProperty("hardcore", String.valueOf(chkHardcore.isSelected()));

        try {
            Path serverDir = context.getProcessService().getServerDirectory();
            context.getConfigFileService().saveServerProperties(serverDir, currentProperties);
            context.getModerationService().logAudit(context.getCurrentAdmin(), "CONFIG_CAMBIO",
                    "Actualizado server.properties (puerto=" + port + ", mundo=" + levelName + ", view=" + viewDist + ")");
            AlertHelper.showInfo("Configuración guardada",
                    "server.properties guardado correctamente.\nReiniciá el servidor para aplicar los cambios.");
        } catch (Exception ex) {
            AlertHelper.showError("Error guardando server.properties", ex.getMessage());
        }
    }

    private boolean getBool(String key, boolean defaultValue) {
        return Boolean.parseBoolean(currentProperties.getProperty(key, String.valueOf(defaultValue)));
    }

    private int parseRange(String raw, int min, int max) {
        int val = Integer.parseInt(raw.trim());
        if (val < min || val > max) {
            throw new NumberFormatException();
        }
        return val;
    }
}
