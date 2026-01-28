package com.wxrley.client;

import com.wxrley.utils.AlertUtils;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * Controla a tela de login e validação de entrada.
 * Carrega e transiciona para ChatController após conexão bem-sucedida.
 */
public class LoginController {

    private static final String IP_PATTERN = "^(\\d{1,3}\\.){3}\\d{1,3}$";
    private static final String LOCALHOST = "localhost";

    @FXML
    private TextField nameField;
    @FXML
    private TextField ipField;
    @FXML
    private Button connectButton;

    /**
     * Valida entradas do usuário e inicia conexão com o servidor.
     * Desabilita controles durante conexão para evitar múltiplas tentativas.
     * Executado ao clicar no botão Conectar.
     */
    @FXML
    private void onConnect() {
        String name = nameField.getText().trim();
        String ip = ipField.getText().trim();

        if (!validateInputs(name, ip)) {
            return;
        }

        setControlsDisabled(true);
        connectAsync(name, ip);
    }

    /**
     * Valida se nome e IP foram preenchidos corretamente.
     * Verifica formato do IP usando regex e valida octetos.
     * Exibe alertas via AlertUtils em caso de erro.
     */
    private boolean validateInputs(String name, String ip) {
        if (name.isEmpty() || ip.isEmpty()) {
            AlertUtils.showError("Dados Incompletos", "Por favor, preencha o Nome e o IP para continuar.");
            return false;
        }

        if (!isValidIp(ip)) {
            AlertUtils.showError("Formato Inválido", "O IP digitado não é um endereço válido (ex: 127.0.0.1).");
            return false;
        }

        return true;
    }

    /**
     * Valida formato do endereço IP usando regex e verifica octetos (0-255).
     * Aceita "localhost" como entrada válida.
     */
    private boolean isValidIp(String ip) {
        if (LOCALHOST.equals(ip)) return true;
        if (!ip.matches(IP_PATTERN)) return false;

        for (String octet : ip.split("\\.")) {
            int value = Integer.parseInt(octet);
            if (value < 0 || value > 255) return false;
        }
        return true;
    }

    /**
     * Conecta ao servidor em thread separada para não travar a interface.
     * Carrega a tela de chat e chama ChatController.initData() para estabelecer conexão.
     * Se conexão falhar, chama handleConnectionError() para exibir erro ao usuário.
     * Após sucesso, troca a cena para o chat via Platform.runLater.
     */
    private void connectAsync(String name, String ip) {
        Thread.ofVirtual().start(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/chat.fxml"));
                Scene chatScene = new Scene(loader.load());
                ChatController chatController = loader.getController();

                chatController.initData(name, ip);

                Platform.runLater(() -> {
                    chatScene.getStylesheets().add(getClass().getResource("/css/theme.css").toExternalForm());

                    Stage stage = (Stage) nameField.getScene().getWindow();

                    double currentWidth = stage.getWidth();
                    double currentHeight = stage.getHeight();

                    stage.setScene(chatScene);
                    stage.setTitle("Chat - " + name);

                    stage.setWidth(currentWidth);
                    stage.setHeight(currentHeight);

                    chatController.setStage(stage);
                });

            } catch (Exception e) {
                Platform.runLater(() -> handleConnectionError(e));
            }
        });
    }

    /**
     * Processa erros de conexão e exibe alerta apropriado.
     * Diferencia erros de autenticação (prefixo "ERRO") de falhas de rede.
     * Reabilita controles para permitir nova tentativa.
     * Chamado por connectAsync() quando conexão falha.
     */
    private void handleConnectionError(Exception e) {
        String errorMessage = e.getMessage();

        if (errorMessage != null && errorMessage.startsWith("ERRO")) {
            AlertUtils.showError("Acesso Negado", errorMessage.replace("ERRO: ", ""));
        } else {
            AlertUtils.showError("Falha na Rede", "Não foi possível conectar. Verifique se o servidor está ativo.");
        }

        setControlsDisabled(false);
    }

    /**
     * Habilita ou desabilita campos de entrada e botão.
     */
    private void setControlsDisabled(boolean disabled) {
        nameField.setDisable(disabled);
        ipField.setDisable(disabled);
        connectButton.setDisable(disabled);
    }
}