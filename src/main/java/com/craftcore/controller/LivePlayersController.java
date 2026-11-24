package com.craftcore.controller;

import com.craftcore.app.AppContext;
import com.craftcore.model.dto.OnlinePlayer;
import com.craftcore.service.ModerationService;
import com.craftcore.util.AlertHelper;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * Controlador [P2]: TableView de jugadores conectados y ContextMenu de moderación 1-clic.
 */
public class LivePlayersController {

    @FXML private Label lblOnlineCount;
    @FXML private TextField txtTargetPlayer;
    @FXML private TextField txtReason;
    @FXML private TableView<OnlinePlayer> tblOnlinePlayers;
    @FXML private TableColumn<OnlinePlayer, String> colAvatar;
    @FXML private TableColumn<OnlinePlayer, String> colNickname;
    @FXML private TableColumn<OnlinePlayer, String> colUuid;
    @FXML private TableColumn<OnlinePlayer, Number> colPing;

    private final AppContext context = AppContext.getInstance();
    private final ModerationService moderationService = context.getModerationService();

    @FXML
    public void initialize() {
        colAvatar.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getAvatarUrl()));
        colAvatar.setCellFactory(col -> new TableCell<>() {
            private final ImageView imageView = new ImageView();

            {
                imageView.setFitWidth(28);
                imageView.setFitHeight(28);
                imageView.setPreserveRatio(true);
            }

            @Override
            protected void updateItem(String url, boolean empty) {
                super.updateItem(url, empty);
                if (empty || url == null || url.isBlank()) {
                    setGraphic(null);
                } else {
                    imageView.setImage(new Image(url, true));
                    setGraphic(imageView);
                }
            }
        });

        colNickname.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNickname()));
        colUuid.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUuid()));
        colPing.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getPing()));

        tblOnlinePlayers.setItems(context.getOnlinePlayers());
        tblOnlinePlayers.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                txtTargetPlayer.setText(newVal.getNickname());
            }
        });

        setupRowContextMenu();
        updateOnlineCount();
        context.getOnlinePlayers().addListener((ListChangeListener<OnlinePlayer>) c -> updateOnlineCount());
    }

    private void setupRowContextMenu() {
        tblOnlinePlayers.setRowFactory(tv -> {
            TableRow<OnlinePlayer> row = new TableRow<>();
            ContextMenu menu = new ContextMenu();

            MenuItem kickItem = new MenuItem("Expulsar (/kick)");
            kickItem.setOnAction(e -> applyModerationFromRow(row.getItem(), "KICK"));

            MenuItem banItem = new MenuItem("Banear (/ban)");
            banItem.setOnAction(e -> applyModerationFromRow(row.getItem(), "BAN"));

            MenuItem warnItem = new MenuItem("Advertir (/warn)");
            warnItem.setOnAction(e -> applyModerationFromRow(row.getItem(), "WARN"));

            MenuItem opItem = new MenuItem("Otorgar OP (/op)");
            opItem.setOnAction(e -> toggleOpForPlayer(row.getItem().getNickname(), row.getItem().getUuid(), true));

            MenuItem deopItem = new MenuItem("Quitar OP (/deop)");
            deopItem.setOnAction(e -> toggleOpForPlayer(row.getItem().getNickname(), row.getItem().getUuid(), false));

            MenuItem survivalItem = new MenuItem("Gamemode Survival");
            survivalItem.setOnAction(e -> setGamemodeForPlayer(row.getItem().getNickname(), "survival"));

            MenuItem creativeItem = new MenuItem("Gamemode Creative");
            creativeItem.setOnAction(e -> setGamemodeForPlayer(row.getItem().getNickname(), "creative"));

            menu.getItems().addAll(kickItem, banItem, warnItem, new SeparatorMenuItem(),
                    opItem, deopItem, new SeparatorMenuItem(), survivalItem, creativeItem);

            row.contextMenuProperty().bind(
                    javafx.beans.binding.Bindings.when(row.emptyProperty())
                            .then((ContextMenu) null)
                            .otherwise(menu)
            );
            return row;
        });
    }

    private void applyModerationFromRow(OnlinePlayer player, String tipo) {
        if (player == null) {
            return;
        }
        AlertHelper.showTextInput("Aplicar " + tipo, "Motivo para " + player.getNickname() + ":", "Incumplimiento de normas")
                .ifPresent(reason -> executeSanction(player.getNickname(), player.getUuid(), tipo, reason));
    }

    @FXML
    private void handleQuickGamemode(ActionEvent event) {
        if (event.getSource() instanceof MenuItem item && item.getUserData() instanceof String gamemode) {
            String target = resolveTargetPlayer();
            if (target != null) {
                setGamemodeForPlayer(target, gamemode);
            }
        }
    }

    @FXML
    private void handleQuickOperator(ActionEvent event) {
        if (event.getSource() instanceof MenuItem item && item.getUserData() instanceof String grantStr) {
            String target = resolveTargetPlayer();
            if (target != null) {
                toggleOpForPlayer(target, resolveSelectedUuid(target), Boolean.parseBoolean(grantStr));
            }
        }
    }

    @FXML
    private void handleQuickKick() {
        applyQuickSanction("KICK");
    }

    @FXML
    private void handleQuickBan() {
        applyQuickSanction("BAN");
    }

    @FXML
    private void handleQuickWarn() {
        applyQuickSanction("WARN");
    }

    private void applyQuickSanction(String tipo) {
        String target = resolveTargetPlayer();
        if (target == null) {
            return;
        }
        String reason = txtReason.getText() == null || txtReason.getText().isBlank()
                ? "Moderación administrativa"
                : txtReason.getText().trim();

        executeSanction(target, resolveSelectedUuid(target), tipo, reason);
        txtReason.clear();
    }

    private String resolveTargetPlayer() {
        String target = txtTargetPlayer.getText() == null ? "" : txtTargetPlayer.getText().trim();
        if (target.isEmpty()) {
            AlertHelper.showWarning("Jugador requerido", "Seleccioná un jugador de la tabla o escribí su nickname.");
            return null;
        }
        return target;
    }

    private String resolveSelectedUuid(String target) {
        OnlinePlayer selected = tblOnlinePlayers.getSelectionModel().getSelectedItem();
        return (selected != null && selected.getNickname().equalsIgnoreCase(target)) ? selected.getUuid() : null;
    }

    private void setGamemodeForPlayer(String nickname, String gamemode) {
        moderationService.changeGamemode(nickname, gamemode, context.getCurrentAdmin());
        AlertHelper.showInfo("Modo de juego actualizado", nickname + " ahora está en modo " + gamemode + ".");
    }

    private void toggleOpForPlayer(String nickname, String uuid, boolean grantOp) {
        moderationService.toggleOperator(nickname, uuid, grantOp, context.getCurrentAdmin());
        AlertHelper.showInfo("Permisos actualizados",
                (grantOp ? "Se otorgó OP a " : "Se quitó OP a ") + nickname + ".");
    }

    private void executeSanction(String nickname, String uuid, String tipo, String reason) {
        try {
            moderationService.applySanction(nickname, uuid, tipo, reason, context.getCurrentAdmin());
            if ("KICK".equals(tipo) || "BAN".equals(tipo)) {
                context.getOnlinePlayers().removeIf(p -> p.getNickname().equalsIgnoreCase(nickname));
            }
            AlertHelper.showInfo("Sanción registrada", tipo + " aplicado a " + nickname + " y guardado en PostgreSQL.");
        } catch (Exception ex) {
            AlertHelper.showError("Error de moderación", ex.getMessage());
        }
    }

    private void updateOnlineCount() {
        int count = context.getOnlinePlayers().size();
        lblOnlineCount.setText(count + (count == 1 ? " jugador en línea" : " jugadores en línea"));
    }
}
