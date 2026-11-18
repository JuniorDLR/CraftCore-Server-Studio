package com.craftcore.controller;

import com.craftcore.app.AppContext;
import com.craftcore.model.dto.OnlinePlayer;
import com.craftcore.service.ModerationService;
import com.craftcore.util.AlertHelper;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ListChangeListener;
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
            opItem.setOnAction(e -> {
                OnlinePlayer p = row.getItem();
                moderationService.toggleOperator(p.getNickname(), p.getUuid(), true, context.getCurrentAdmin());
                AlertHelper.showInfo("OP otorgado", "Se ejecutó /op para " + p.getNickname());
            });

            MenuItem survivalItem = new MenuItem("Gamemode Survival");
            survivalItem.setOnAction(e -> moderationService.changeGamemode(
                    row.getItem().getNickname(), "survival", context.getCurrentAdmin()));

            MenuItem creativeItem = new MenuItem("Gamemode Creative");
            creativeItem.setOnAction(e -> moderationService.changeGamemode(
                    row.getItem().getNickname(), "creative", context.getCurrentAdmin()));

            menu.getItems().addAll(kickItem, banItem, warnItem, new SeparatorMenuItem(), opItem, survivalItem, creativeItem);

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
        String target = txtTargetPlayer.getText() == null ? "" : txtTargetPlayer.getText().trim();
        if (target.isEmpty()) {
            AlertHelper.showWarning("Jugador requerido", "Ingresá o seleccioná el nickname del jugador.");
            return;
        }
        String reason = txtReason.getText() == null || txtReason.getText().isBlank()
                ? "Moderación administrativa"
                : txtReason.getText().trim();

        OnlinePlayer selected = tblOnlinePlayers.getSelectionModel().getSelectedItem();
        String uuid = (selected != null && selected.getNickname().equalsIgnoreCase(target)) ? selected.getUuid() : null;
        executeSanction(target, uuid, tipo, reason);
        txtReason.clear();
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
