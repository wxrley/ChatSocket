package com.wxrley.server;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;

/**
 * Controla a interface gráfica do servidor.
 * Gerencia botão de ligar/desligar e exibe logs na interface.
 */
public class ServerController {

    private static final int SERVER_PORT = 3000;

    @FXML
    private Button toggleButton;
    @FXML
    private ListView<String> logList;

    private Server server;

    /**
     * Configura auto-scroll da lista de logs.
     * Executado automaticamente pelo JavaFX ao carregar a interface.
     */
    @FXML
    public void initialize() {
        logList.getItems().addListener((ListChangeListener<String>) change ->
                Platform.runLater(() -> {
                    if (!logList.getItems().isEmpty()) {
                        logList.scrollTo(logList.getItems().size() - 1);
                    }
                })
        );
    }

    /**
     * Alterna entre ligar e desligar o servidor.
     * Cria instância de Server com callback que adiciona logs na interface via Platform.runLater.
     * Chama Server.start() para iniciar ou stopServer() para parar.
     * Executado ao clicar no botão Ligar/Desligar.
     */
    @FXML
    private void onToggleServer() {
        if (server == null || !server.isRunning()) {
            server = new Server(SERVER_PORT, message ->
                    Platform.runLater(() -> logList.getItems().add(message))
            );
            server.start();
            toggleButton.setText("Desligar");
        } else {
            stopServer();
        }
    }

    /**
     * Para o servidor e atualiza texto do botão.
     * Chama Server.stop() para desconectar todos os clientes.
     * Chamado por onToggleServer() ou ServerFXMain ao fechar janela.
     */
    public void stopServer() {
        if (server != null && server.isRunning()) {
            server.stop();
            toggleButton.setText("Ligar");
        }
    }
}