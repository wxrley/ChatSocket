package com.wxrley.server;

import com.wxrley.model.Message;
import com.wxrley.model.User;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Servidor TCP que gerencia múltiplos clientes conectados simultaneamente.
 * Utiliza ConcurrentHashMap para thread-safety e virtual threads para escalabilidade.
 * Controlado por ServerController para ligar/desligar via interface.
 */
public class Server {

    public static final User SYSTEM_USER = new User("Sistema");

    private final int port;
    private final Consumer<String> logCallback;
    private final Map<String, ClientHandler> clients;

    private ServerSocket serverSocket;
    private volatile boolean running;

    /**
     * Inicializa servidor com porta e callback para logs.
     * O logCallback envia logs para ServerController exibir na interface.
     */
    public Server(int port, Consumer<String> logCallback) {
        this.port = port;
        this.logCallback = logCallback;
        this.clients = new ConcurrentHashMap<>();
    }

    /**
     * Inicia o servidor TCP e começa a aceitar conexões.
     * Cria thread virtual que aceita conexões em loop.
     * Para cada conexão, cria um ClientHandler em nova thread virtual.
     * Chamado por ServerController.onToggleServer().
     */
    public void start() {
        if (running) return;

        Thread.ofVirtual().start(() -> {
            try {
                serverSocket = new ServerSocket(port);
                running = true;
                log("[SERVIDOR] Iniciado na porta " + port);

                while (running) {
                    try {
                        Socket socket = serverSocket.accept();
                        Thread.ofVirtual().start(new ClientHandler(socket, this));
                    } catch (Exception e) {
                        if (running) {
                            log("[ERRO] Falha ao aceitar conexão: " + e.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                log("[ERRO] Falha ao iniciar servidor: " + e.getMessage());
            }
        });
    }

    /**
     * Para o servidor e desconecta todos os clientes.
     * Copia lista de clientes antes de limpar para evitar ConcurrentModificationException.
     * Chama ClientHandler.forceDisconnect() para cada cliente conectado.
     * Fecha o ServerSocket para liberar a porta.
     * Chamado por ServerController.stopServer().
     */
    public void stop() {
        if (!running) return;

        running = false;

        List<ClientHandler> clientList = new ArrayList<>(clients.values());
        clients.clear();

        for (ClientHandler client : clientList) {
            client.forceDisconnect();
        }

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (Exception ignored) {
        }

        log("[SERVIDOR] Desligado");
        log("");
    }

    /**
     * Registra novo cliente conectado no mapa (thread-safe).
     * Chamado por ClientHandler.run() após autenticação bem-sucedida.
     */
    public void addClient(ClientHandler handler) {
        String userName = handler.getUser().name();
        clients.put(userName, handler);
        log("[CONEXÃO] " + userName);
        log("[TOTAL ONLINE] " + clients.size());
    }

    /**
     * Remove cliente desconectado do mapa.
     * Chamado por ClientHandler.disconnect() quando cliente sai.
     */
    public void removeClient(ClientHandler handler) {
        if (handler.getUser() != null) {
            String userName = handler.getUser().name();
            clients.remove(userName);
            log("[DESCONEXÃO] " + userName);
            log("[TOTAL ONLINE] " + clients.size());
        }
    }

    /**
     * Verifica se um nome de usuário já está em uso.
     * Consultado por ClientHandler.authenticate() ao validar novo cliente.
     */
    public boolean isUserOnline(String name) {
        return clients.containsKey(name);
    }

    /**
     * Verifica se o servidor está rodando.
     * Consultado por ServerController.onToggleServer().
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Envia mensagem para todos os clientes conectados.
     * Chama ClientHandler.sendMessage() para cada cliente no mapa.
     * Chamado por ClientHandler.listenForMessages() ao receber mensagem de cliente.
     */
    public void broadcast(Message message) {
        clients.values().forEach(client -> client.sendMessage(message));
    }

    /**
     * Envia mensagem para todos os clientes exceto um específico.
     * Usado para notificar entrada de usuário sem enviar para ele mesmo.
     * Chamado por ClientHandler.run() ao notificar entrada de novo usuário.
     */
    public void broadcastExcept(Message message, ClientHandler excluded) {
        clients.values().stream()
                .filter(client -> client != excluded)
                .forEach(client -> client.sendMessage(message));
    }

    /**
     * Envia log para o callback da interface.
     * O callback adiciona mensagem no ListView do ServerController.
     */
    private void log(String message) {
        if (logCallback != null) {
            logCallback.accept(message);
        }
    }
}