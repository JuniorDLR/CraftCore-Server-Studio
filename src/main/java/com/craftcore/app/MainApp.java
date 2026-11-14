package com.craftcore.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Aplicación principal JavaFX: CraftCore Server Studio.
 * Orquestador visual para servidores dedicados de Minecraft.
 */
public class MainApp extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        stage.setTitle("CraftCore Server Studio — Panel de Administración");
        stage.setWidth(1100);
        stage.setHeight(720);
        stage.setMinWidth(960);
        stage.setMinHeight(600);

        try {
            // Intentar cargar la vista del Dashboard principal si existe
            java.net.URL fxmlLocation = getClass().getResource("/com/craftcore/fxml/DashboardView.fxml");
            if (fxmlLocation != null) {
                FXMLLoader loader = new FXMLLoader(fxmlLocation);
                Parent root = loader.load();
                Scene scene = new Scene(root);
                stage.setScene(scene);
            } else {
                // Fallback temporal si aún no se han creado las vistas FXML
                javafx.scene.layout.StackPane root = new javafx.scene.layout.StackPane();
                javafx.scene.control.Label placeholder = new javafx.scene.control.Label("CraftCore Server Studio cargando...");
                root.getChildren().add(placeholder);
                stage.setScene(new Scene(root, 1024, 680));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        stage.show();
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
