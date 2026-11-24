package com.craftcore.controller;

import com.craftcore.app.AppContext;
import com.craftcore.dao.jdbc.AuditLogJdbcDAO;
import com.craftcore.dao.orm.SanctionDAO;
import com.craftcore.model.entity.Admin;
import com.craftcore.model.entity.AuditLog;
import com.craftcore.model.entity.Sanction;
import com.craftcore.util.AlertHelper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controlador [P4]: Consulta y gestión CRUD de sanciones (Hibernate) y auditoría (JDBC).
 */
public class AuditHistoryController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML private DatePicker dpSanctionDesde;
    @FXML private DatePicker dpSanctionHasta;
    @FXML private ComboBox<String> cmbSanctionTipo;
    @FXML private TableView<Sanction> tblSanctions;
    @FXML private TableColumn<Sanction, Integer> colSanctionId;
    @FXML private TableColumn<Sanction, String> colSanctionPlayer;
    @FXML private TableColumn<Sanction, String> colSanctionTipo;
    @FXML private TableColumn<Sanction, String> colSanctionMotivo;
    @FXML private TableColumn<Sanction, String> colSanctionEstado;
    @FXML private TableColumn<Sanction, String> colSanctionAdmin;
    @FXML private TableColumn<Sanction, String> colSanctionFecha;

    @FXML private DatePicker dpLogDesde;
    @FXML private DatePicker dpLogHasta;
    @FXML private ComboBox<String> cmbLogAccion;
    @FXML private TableView<AuditLog> tblAuditLogs;
    @FXML private TableColumn<AuditLog, Long> colLogId;
    @FXML private TableColumn<AuditLog, String> colLogAccion;
    @FXML private TableColumn<AuditLog, String> colLogDetalle;
    @FXML private TableColumn<AuditLog, String> colLogAdmin;
    @FXML private TableColumn<AuditLog, String> colLogFecha;

    private final SanctionDAO sanctionDAO = new SanctionDAO();
    private final AuditLogJdbcDAO auditLogDAO = new AuditLogJdbcDAO();
    private final AppContext context = AppContext.getInstance();

    @FXML
    public void initialize() {
        cmbSanctionTipo.setItems(FXCollections.observableArrayList("TODOS", "BAN", "KICK", "WARN", "MUTE"));
        cmbSanctionTipo.getSelectionModel().selectFirst();

        cmbLogAccion.setItems(FXCollections.observableArrayList(
                "TODOS", "INICIO_SRV", "STOP_SRV", "EJECUCION_COMANDO", "WHITELIST_ADD", "CONFIG_CAMBIO"));
        cmbLogAccion.getSelectionModel().selectFirst();

        setupSanctionColumns();
        setupLogColumns();
        loadSanctions();
        loadAuditLogs();
    }

    private void setupSanctionColumns() {
        colSanctionId.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getIdSancion()));
        colSanctionPlayer.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getJugador() != null ? d.getValue().getJugador().getNickname() : "—"));
        colSanctionTipo.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTipo()));
        colSanctionMotivo.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getMotivo()));
        colSanctionEstado.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEstado()));
        colSanctionAdmin.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getAdmin() != null ? d.getValue().getAdmin().getUsername() : "—"));
        colSanctionFecha.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getFechaInicio() != null ? d.getValue().getFechaInicio().format(DATE_FMT) : "—"));
    }

    private void setupLogColumns() {
        colLogId.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getIdLog()));
        colLogAccion.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getAccion()));
        colLogDetalle.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDetalle()));
        colLogAdmin.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getAdmin() != null ? d.getValue().getAdmin().getUsername() : "Sistema"));
        colLogFecha.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getFecha() != null ? d.getValue().getFecha().format(DATE_FMT) : "—"));
    }

    @FXML
    public void loadSanctions() {
        try {
            List<Sanction> list = sanctionDAO.findByFilters(
                    dpSanctionDesde.getValue(), dpSanctionHasta.getValue(), cmbSanctionTipo.getValue());
            tblSanctions.setItems(FXCollections.observableArrayList(list));
        } catch (Exception ex) {
            AlertHelper.showError("Error cargando sanciones", ex.getMessage());
        }
    }

    @FXML
    private void clearSanctionFilters() {
        dpSanctionDesde.setValue(null);
        dpSanctionHasta.setValue(null);
        cmbSanctionTipo.getSelectionModel().selectFirst();
        loadSanctions();
    }

    @FXML
    private void handleRevokeSanction() {
        Sanction selected = tblSanctions.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertHelper.showWarning("Selección requerida", "Seleccioná una sanción para revocar.");
            return;
        }
        selected.setEstado("REVOCADO");
        sanctionDAO.update(selected);
        if ("BAN".equalsIgnoreCase(selected.getTipo()) && selected.getJugador() != null) {
            context.getProcessService().sendCommand("pardon " + selected.getJugador().getNickname());
        }
        loadSanctions();
    }

    @FXML
    private void handleDeleteSanction() {
        Sanction selected = tblSanctions.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertHelper.showWarning("Selección requerida", "Seleccioná una sanción para eliminar.");
            return;
        }
        if (AlertHelper.showConfirmation("Eliminar sanción", "¿Eliminar el registro #" + selected.getIdSancion() + "?")) {
            sanctionDAO.deleteById(selected.getIdSancion());
            loadSanctions();
        }
    }

    @FXML
    public void loadAuditLogs() {
        try {
            List<AuditLog> logs = auditLogDAO.findByFilters(
                    dpLogDesde.getValue(), dpLogHasta.getValue(), cmbLogAccion.getValue());
            tblAuditLogs.setItems(FXCollections.observableArrayList(logs));
        } catch (Exception ex) {
            AlertHelper.showError("Error JDBC en auditoría", ex.getMessage());
        }
    }

    @FXML
    private void clearLogFilters() {
        dpLogDesde.setValue(null);
        dpLogHasta.setValue(null);
        cmbLogAccion.getSelectionModel().selectFirst();
        loadAuditLogs();
    }

    @FXML
    private void handleAddManualLog() {
        AlertHelper.showTextInput("Nueva Nota de Auditoría", "Detalle del evento administrativo:", "")
                .ifPresent(detail -> {
                    try {
                        Admin admin = context.getCurrentAdmin();
                        auditLogDAO.insert(admin != null ? admin.getIdAdmin() : null, "CONFIG_CAMBIO", detail);
                        loadAuditLogs();
                    } catch (Exception ex) {
                        AlertHelper.showError("Error insertando log", ex.getMessage());
                    }
                });
    }

    @FXML
    private void handleDeleteLog() {
        AuditLog selected = tblAuditLogs.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertHelper.showWarning("Selección requerida", "Seleccioná un log para eliminar.");
            return;
        }
        if (AlertHelper.showConfirmation("Eliminar log", "¿Eliminar el log #" + selected.getIdLog() + "?")) {
            try {
                auditLogDAO.deleteById(selected.getIdLog());
                loadAuditLogs();
            } catch (Exception ex) {
                AlertHelper.showError("Error eliminando log", ex.getMessage());
            }
        }
    }
}
