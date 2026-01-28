package com.wxrley.server;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Classe principal da aplicação servidor JavaFX.
 * Carrega a interface do servidor e inicializa o stage principal.
 */
public class ServerFXMain extends Application {

    /**
     * Inicializa a aplicação JavaFX do servidor.
     * Carrega interface via FXML, aplica CSS e configura encerramento.
     * Chama ServerController.stopServer() ao fechar para desligar servidor graciosamente.
     */
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/server.fxml"));
        Scene scene = new Scene(loader.load());
        scene.getStylesheets().add(getClass().getResource("/css/theme.css").toExternalForm());

        ServerController controller = loader.getController();

        stage.setTitle("ChatSocket - Servidor");
        stage.setScene(scene);
        stage.setMinWidth(500);
        stage.setMinHeight(500);
        stage.setOnCloseRequest(event -> {
            controller.stopServer();
            Platform.exit();
            System.exit(0);
        });
        stage.show();
    }

    /**
     * Ponto de entrada da aplicação.
     */
    public static void main(String[] args) {
        launch(args);
    }
}