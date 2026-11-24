package com.craftcore.app;

import com.craftcore.util.HibernateUtil;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

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
        stage.setWidth(1160);
        stage.setHeight(740);
        stage.setMinWidth(980);
        stage.setMinHeight(620);

        try {
            URL fxmlLocation = getClass().getResource("/com/craftcore/fxml/DashboardView.fxml");
            if (fxmlLocation != null) {
                FXMLLoader loader = new FXMLLoader(fxmlLocation);
                Parent root = loader.load();
                Scene scene = new Scene(root);
                stage.setScene(scene);
            } else {
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

    @Override
    public void stop() {
        AppContext.getInstance().getProcessService().shutdown();
        HibernateUtil.shutdown();
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
