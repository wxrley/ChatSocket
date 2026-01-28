package com.wxrley.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Classe principal da aplicação cliente JavaFX.
 * Carrega a tela de login e inicializa a interface gráfica.
 */
public class ClientFXMain extends Application {

    /**
     * Inicializa a aplicação JavaFX e carrega a tela de login.
     * Configura o stage principal com tema CSS e encerramento completo ao fechar.
     */
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
        Scene scene = new Scene(loader.load());
        scene.getStylesheets().add(getClass().getResource("/css/theme.css").toExternalForm());

        stage.setTitle("ChatSocket");
        stage.setScene(scene);
        stage.setMinWidth(500);
        stage.setMinHeight(500);
        stage.setOnCloseRequest(event -> {
            Platform.exit();
            System.exit(0);
        });
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}