package com.wxrley.client;

import com.wxrley.model.Message;
import com.wxrley.model.User;
import com.wxrley.server.Server;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

/**
 * Controla a interface de chat e gerencia comunicação com o servidor.
 * Utiliza Connection para enviar/receber mensagens em tempo real.
 */
public class ChatController {

    private static final int SERVER_PORT = 3000;

    @FXML
    private ListView<Message> chatList;
    @FXML
    private TextArea messageField;

    private User user;
    private Connection connection;

    /**
     * Configura auto-scroll da lista de mensagens e comportamento de teclas no campo de entrada.
     * Define que Enter envia mensagem e Shift+Enter quebra linha.
     * O campo de texto cresce automaticamente até 3 linhas.
     * Executado automaticamente pelo JavaFX ao carregar a interface.
     */
    @FXML
    public void initialize() {
        chatList.getItems().addListener((ListChangeListener<Message>) change ->
                Platform.runLater(() -> {
                    if (!chatList.getItems().isEmpty()) {
                        chatList.scrollTo(chatList.getItems().size() - 1);
                    }
                })
        );

        messageField.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                if (event.isShiftDown()) {
                    int caretPosition = messageField.getCaretPosition();
                    messageField.insertText(caretPosition, "\n");
                    event.consume();
                } else {
                    event.consume();
                    onSend();
                }
            }
        });

        // Listener para crescer o campo dinamicamente (até 3 linhas)
        messageField.textProperty().addListener((observable, oldValue, newValue) -> {
            int lineCount = newValue.split("\n", -1).length;
            int maxLines = 3;
            int visibleLines = Math.min(lineCount, maxLines);

            messageField.setPrefRowCount(visibleLines);
        });
    }

    /**
     * Inicializa dados do usuário e conecta ao servidor.
     * Configura MessageCell para renderizar as mensagens.
     * Chamado por LoginController.connectAsync() após validação de entrada.
     */
    public void initData(String userName, String serverIp) throws Exception {
        this.user = new User(userName);
        chatList.setCellFactory(param -> new MessageCell(user.name()));
        connectToServer(serverIp);
    }

    /**
     * Configura encerramento da conexão ao fechar janela.
     * Chama Connection.close() para liberar recursos.
     * Chamado por LoginController.connectAsync() ao trocar de cena.
     */
    public void setStage(Stage stage) {
        stage.setOnCloseRequest(event -> {
            if (connection != null) {
                connection.close();
            }
            Platform.exit();
        });
    }

    /**
     * Envia mensagem ao servidor quando usuário pressiona Enter ou clica em Enviar.
     * Verifica se há conexão ativa antes de enviar.
     * Chama Connection.send() para transmitir a mensagem.
     */
    @FXML
    private void onSend() {
        if (connection == null || !connection.isConnected()) {
            return;
        }

        String text = messageField.getText().trim();
        if (text.isEmpty()) {
            return;
        }

        try {
            connection.send(new Message(user, text));
            messageField.clear();
            messageField.requestFocus();
        } catch (Exception ignored) {
        }
    }

    /**
     * Estabelece conexão com o servidor.
     * Define callbacks para receber mensagens e notificações de status.
     * As mensagens recebidas são adicionadas na chatList via Platform.runLater.
     * Chama Connection.connect() para iniciar socket TCP.
     */
    private void connectToServer(String serverIp) throws Exception {
        connection = new Connection();

        connection.connect(
                serverIp,
                SERVER_PORT,
                new Message(user, ""),
                message -> Platform.runLater(() -> chatList.getItems().add(message)),
                status -> handleConnectionStatus(status)
        );
    }

    /**
     * Processa mudanças de status da conexão (DISCONNECTED, RECONNECTED, FAILED).
     * Adiciona mensagens do sistema na interface informando o estado da conexão.
     * Chamado por Connection via statusCallback quando estado muda.
     */
    private void handleConnectionStatus(String status) {
        Platform.runLater(() -> {
            switch (status) {
                case "DISCONNECTED":
                    chatList.getItems().add(new Message(Server.SYSTEM_USER, "Servidor desconectado."));
                    break;
                case "RECONNECTED":
                    chatList.getItems().add(new Message(Server.SYSTEM_USER, "Servidor reconectado."));
                    break;
                case "FAILED":
                    chatList.getItems().add(new Message(Server.SYSTEM_USER, "Falha ao reconectar. Feche e tente novamente."));
                    break;
            }
        });
    }
}