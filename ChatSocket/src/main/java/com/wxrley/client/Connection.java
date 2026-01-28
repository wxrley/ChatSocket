package com.wxrley.client;

import com.wxrley.model.Message;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * Gerencia conexão TCP com o servidor e reconexão automática.
 * Utilizada por ChatController para comunicação em tempo real.
 */
public class Connection {

    private static final int CONNECTION_TIMEOUT = 5000;
    private static final int MAX_RECONNECTION_ATTEMPTS = 10;
    private static final int RECONNECTION_DELAY = 3000;

    private Socket socket;
    private ObjectOutputStream output;
    private ObjectInputStream input;

    private String serverIp;
    private int serverPort;
    private Message loginMessage;
    private Consumer<Message> messageCallback;
    private Consumer<String> statusCallback;

    private volatile boolean shouldReconnect = true;
    private volatile boolean isAuthenticationError = false;
    private volatile boolean isFirstConnection = true;

    /**
     * Estabelece conexão com o servidor e inicia recebimento de mensagens.
     * O loginMsg é enviado e validado por ClientHandler.authenticate() no servidor.
     * Chamado por ChatController.connectToServer().
     */
    public void connect(String ip, int port, Message loginMsg, Consumer<Message> onMessage, Consumer<String> onStatus) throws Exception {
        this.serverIp = ip;
        this.serverPort = port;
        this.loginMessage = loginMsg;
        this.messageCallback = onMessage;
        this.statusCallback = onStatus;

        connectToServer();
        startListening();
    }

    /**
     * Envia mensagem ao servidor de forma thread-safe.
     * Chamado por ChatController.onSend().
     */
    public synchronized void send(Message message) throws Exception {
        if (!isConnected()) {
            throw new Exception("Não conectado ao servidor");
        }

        output.writeObject(message);
        output.flush();
        output.reset();
    }

    /**
     * Verifica se há conexão TCP ativa.
     */
    public boolean isConnected() {
        return socket != null && !socket.isClosed() && socket.isConnected() && !socket.isInputShutdown();
    }

    /**
     * Encerra conexão e impede tentativas de reconexão.
     * Chamado por ChatController.setStage() ao fechar janela.
     */
    public void close() {
        shouldReconnect = false;
        closeResources();
    }

    /**
     * Cria socket TCP, autentica usuário e prepara para receber mensagens.
     * Envia flag indicando se é reconexão (!isFirstConnection) para evitar notificação duplicada de entrada.
     * O servidor valida em ClientHandler.authenticate() e retorna confirmação ou erro com prefixo "ERRO".
     * Se autenticação falhar, fecha conexão e lança exceção.
     * Se não for primeira conexão, notifica reconexão via statusCallback.
     */
    private void connectToServer() throws Exception {
        socket = new Socket();
        socket.setTcpNoDelay(true);
        socket.connect(new InetSocketAddress(serverIp, serverPort), CONNECTION_TIMEOUT);

        output = new ObjectOutputStream(socket.getOutputStream());
        input = new ObjectInputStream(socket.getInputStream());

        Message loginWithFlag = new Message(
                loginMessage.getSender(),
                loginMessage.getContent(),
                !isFirstConnection
        );
        send(loginWithFlag);

        Message response = (Message) input.readObject();

        if (response.getContent().startsWith("ERRO")) {
            isAuthenticationError = true;
            close();
            throw new Exception(response.getContent());
        }

        isAuthenticationError = false;
        messageCallback.accept(response);

        if (!isFirstConnection && statusCallback != null) {
            statusCallback.accept("RECONNECTED");
        }

        isFirstConnection = false;
    }

    /**
     * Cria thread virtual que executa listenForMessages().
     * Chamado por connect() e reconnect().
     */
    private void startListening() {
        Thread.ofVirtual().start(() -> listenForMessages());
    }

    /**
     * Recebe mensagens do servidor em loop contínuo.
     * Cada mensagem é enviada via messageCallback para ChatController.
     * Se conexão cair, chama notifyDisconnected() e reconnect().
     */
    private void listenForMessages() {
        while (shouldReconnect) {
            try {
                Message msg = (Message) input.readObject();
                messageCallback.accept(msg);
            } catch (Exception e) {
                if (shouldReconnect && !isAuthenticationError) {
                    notifyDisconnected();
                    reconnect();
                    break;
                }
            }
        }
    }

    /**
     * Notifica desconexão via statusCallback e fecha recursos.
     * Chamado por listenForMessages() quando detecta perda de conexão.
     */
    private void notifyDisconnected() {
        if (statusCallback != null) {
            statusCallback.accept("DISCONNECTED");
        }
        closeResources();
    }

    /**
     * Tenta reconectar ao servidor automaticamente após perda de conexão.
     * Faz até 10 tentativas com 3 segundos de intervalo entre cada uma.
     * Após reconexão bem-sucedida, chama connectToServer() e startListening() para recriar thread.
     * Se esgotar tentativas, notifica falha via statusCallback com "FAILED".
     * Chamado por listenForMessages() quando detecta desconexão.
     */
    private void reconnect() {
        for (int attempt = 1; attempt <= MAX_RECONNECTION_ATTEMPTS && shouldReconnect && !isAuthenticationError; attempt++) {
            try {
                Thread.sleep(RECONNECTION_DELAY);
                connectToServer();
                startListening();
                return;
            } catch (Exception ignored) {
            }
        }

        if (statusCallback != null) {
            statusCallback.accept("FAILED");
        }
        shouldReconnect = false;
    }

    /**
     * Fecha streams e socket TCP.
     */
    private void closeResources() {
        try {
            if (input != null) input.close();
            if (output != null) output.close();
            if (socket != null) socket.close();
        } catch (Exception ignored) {
        }

        input = null;
        output = null;
        socket = null;
    }
}