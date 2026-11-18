package com.craftcore.controller;

import com.craftcore.app.AppContext;
import com.craftcore.dao.orm.PlayerDAO;
import com.craftcore.model.dto.MojangProfileDTO;
import com.craftcore.model.dto.WhitelistEntryDTO;
import com.craftcore.model.entity.Player;
import com.craftcore.util.AlertHelper;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Controlador [P3]: Consumo REST de Mojang API, vista previa de skin y gestión de whitelist.json.
 */
public class WhitelistController {

    @FXML private TextField txtUsername;
    @FXML private Button btnLookup;
    @FXML private Button btnAddWhitelist;
    @FXML private Label lblProfileName;
    @FXML private Label lblProfileUuid;
    @FXML private ImageView imgSkin;
    @FXML private TableView<WhitelistEntryDTO> tblWhitelist;
    @FXML private TableColumn<WhitelistEntryDTO, String> colName;
    @FXML private TableColumn<WhitelistEntryDTO, String> colUuid;

    private final AppContext context = AppContext.getInstance();
    private final PlayerDAO playerDAO = new PlayerDAO();
    private MojangProfileDTO lastProfile;

    @FXML
    public void initialize() {
        colName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        colUuid.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUuid()));
        loadWhitelistTable();
    }

    @FXML
    private void handleLookupMojang() {
        String username = txtUsername.getText() == null ? "" : txtUsername.getText().trim();
        if (username.isEmpty()) {
            AlertHelper.showWarning("Campo vacío", "Ingresá un nickname de Minecraft para consultar.");
            return;
        }

        btnLookup.setDisable(true);
        btnAddWhitelist.setDisable(true);
        lblProfileName.setText("Consultando Mojang...");
        lblProfileUuid.setText("UUID: —");

        new Thread(() -> {
            try {
                Optional<MojangProfileDTO> result = context.getMojangApiService().fetchPlayerProfile(username);
                Platform.runLater(() -> {
                    btnLookup.setDisable(false);
                    if (result.isEmpty()) {
                        lastProfile = null;
                        lblProfileName.setText("Jugador no encontrado (HTTP 404)");
                        imgSkin.setImage(null);
                        return;
                    }
                    lastProfile = result.get();
                    lblProfileName.setText("Jugador: " + lastProfile.getName());
                    lblProfileUuid.setText("UUID: " + lastProfile.getFormattedUuid());
                    imgSkin.setImage(new Image("https://mc-heads.net/avatar/" + lastProfile.getName() + "/72", true));
                    btnAddWhitelist.setDisable(false);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    btnLookup.setDisable(false);
                    lblProfileName.setText("Error de consulta");
                    AlertHelper.showError("Error Mojang API", ex.getMessage());
                });
            }
        }, "mojang-lookup").start();
    }

    @FXML
    private void handleAddToWhitelist() {
        if (lastProfile == null) {
            return;
        }
        String uuid = lastProfile.getFormattedUuid();
        String name = lastProfile.getName();
        Path serverDir = context.getProcessService().getServerDirectory();

        try {
            List<WhitelistEntryDTO> updated = context.getConfigFileService().addToWhitelist(serverDir, uuid, name);
            tblWhitelist.setItems(FXCollections.observableArrayList(updated));

            Player player = playerDAO.findByUuid(uuid).orElseGet(() -> new Player(uuid, name));
            player.setNickname(name);
            player.setSkinUrl(lastProfile.getSkinUrl());
            player.setEnWhitelist(true);
            if (player.getIdJugador() == null) {
                playerDAO.save(player);
            } else {
                playerDAO.update(player);
            }

            if (context.getProcessService().isRunning()) {
                context.getProcessService().sendCommand("whitelist add " + name);
            }

            context.getModerationService().logAudit(context.getCurrentAdmin(), "WHITELIST_ADD",
                    "Jugador agregado: " + name + " (" + uuid + ")");
            AlertHelper.showInfo("Whitelist actualizada", name + " fue agregado a whitelist.json y PostgreSQL.");
        } catch (Exception ex) {
            AlertHelper.showError("Error al agregar", ex.getMessage());
        }
    }

    @FXML
    private void handleRemoveSelected() {
        WhitelistEntryDTO selected = tblWhitelist.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertHelper.showWarning("Selección requerida", "Seleccioná un jugador de la tabla para eliminar.");
            return;
        }
        if (!AlertHelper.showConfirmation("Confirmar eliminación", "¿Eliminar a " + selected.getName() + " de la whitelist?")) {
            return;
        }

        Path serverDir = context.getProcessService().getServerDirectory();
        try {
            List<WhitelistEntryDTO> updated = context.getConfigFileService()
                    .removeFromWhitelist(serverDir, selected.getUuid());
            tblWhitelist.setItems(FXCollections.observableArrayList(updated));

            playerDAO.findByUuid(selected.getUuid()).ifPresent(p -> {
                p.setEnWhitelist(false);
                playerDAO.update(p);
            });

            if (context.getProcessService().isRunning()) {
                context.getProcessService().sendCommand("whitelist remove " + selected.getName());
            }

            context.getModerationService().logAudit(context.getCurrentAdmin(), "WHITELIST_ADD",
                    "Jugador removido de whitelist: " + selected.getName());
        } catch (Exception ex) {
            AlertHelper.showError("Error al eliminar", ex.getMessage());
        }
    }

    @FXML
    public void loadWhitelistTable() {
        try {
            Path serverDir = context.getProcessService().getServerDirectory();
            List<WhitelistEntryDTO> entries = context.getConfigFileService().loadWhitelist(serverDir);
            tblWhitelist.setItems(FXCollections.observableArrayList(entries));
        } catch (Exception ex) {
            AlertHelper.showError("Error leyendo whitelist.json", ex.getMessage());
        }
    }
}
