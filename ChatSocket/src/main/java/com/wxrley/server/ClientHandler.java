package com.wxrley.server;

import com.wxrley.model.Message;
import com.wxrley.model.User;

import java.io.EOFException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;

/**
 * Gerencia a conexão de um cliente específico no servidor.
 * Cada ClientHandler executa em thread virtual separada.
 * Criado por Server.start() ao aceitar nova conexão TCP.
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private final Server server;

    private ObjectOutputStream output;
    private ObjectInputStream input;
    private User user;

    /**
     * Inicializa handler com socket do cliente conectado.
     * Chamado por Server.start() ao aceitar conexão.
     */
    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
    }

    /**
     * Executa o ciclo de vida completo do cliente conectado.
     * Cria streams, lê mensagem de login para verificar se é reconexão, autentica usuário e registra no servidor.
     * Notifica entrada do usuário apenas se NÃO for reconexão (evita spam de "X entrou" após queda do servidor).
     * Se autenticação falhar, encerra imediatamente.
     * Chamado automaticamente pela thread virtual criada em Server.start().
     */
    @Override
    public void run() {
        try {
            output = new ObjectOutputStream(socket.getOutputStream());
            input = new ObjectInputStream(socket.getInputStream());

            Message loginMsg = (Message) input.readObject();
            boolean isReconnection = loginMsg.isReconnection();

            if (!authenticate(loginMsg)) {
                return;
            }

            server.addClient(this);

            sendMessage(new Message(Server.SYSTEM_USER, "Você entrou no chat como " + user.name()));

            if (!isReconnection) {
                server.broadcastExcept(new Message(Server.SYSTEM_USER, user.name() + " entrou."), this);
            }

            listenForMessages();

        } catch (Exception ignored) {
        } finally {
            disconnect();
        }
    }

    /**
     * Envia mensagem ao cliente através do socket.
     * Chamado por Server.broadcast() e Server.broadcastExcept() ao distribuir mensagens.
     */
    public void sendMessage(Message message) {
        try {
            if (output != null && !socket.isClosed()) {
                output.writeObject(message);
                output.flush();
                output.reset();
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Força desconexão do cliente.
     * Chamado por Server.stop() ao desligar o servidor.
     */
    public void forceDisconnect() {
        disconnect();
    }

    /**
     * Consultado por Server.addClient() e Server.removeClient().
     */
    public User getUser() {
        return user;
    }

    /**
     * Autentica o usuário verificando se o nome já está em uso.
     * Recebe mensagem de login (já lida em run()) enviada por Connection.connectToServer().
     * Se nome duplicado, envia erro com prefixo "ERRO" e retorna false.
     * O erro é detectado por Connection.connectToServer() e lançado como exceção.
     */
    private boolean authenticate(Message loginMsg) throws Exception {
        User requestedUser = loginMsg.getSender();

        if (server.isUserOnline(requestedUser.name())) {
            sendMessage(new Message(
                    Server.SYSTEM_USER,
                    "ERRO: O usuário '" + requestedUser.name() + "' já está em uso."
            ));
            return false;
        }

        this.user = requestedUser;
        return true;
    }

    /**
     * Recebe mensagens do cliente em loop contínuo.
     * Cada mensagem recebida é distribuída via Server.broadcast() para todos os clientes.
     * Encerra ao detectar desconexão (EOFException ou SocketException).
     */
    private void listenForMessages() {
        try {
            while (true) {
                Message msg = (Message) input.readObject();
                server.broadcast(msg);
            }
        } catch (EOFException | SocketException e) {
        } catch (Exception ignored) {
        }
    }

    /**
     * Desconecta cliente e libera recursos.
     * Remove cliente do servidor via Server.removeClient() e notifica saída via broadcast.
     * Fecha streams e socket TCP.
     * Chamado por run() no finally ou por forceDisconnect().
     */
    private void disconnect() {
        if (user != null) {
            String userName = user.name();
            server.removeClient(this);
            server.broadcast(new Message(Server.SYSTEM_USER, userName + " saiu."));
            user = null;
        }

        try {
            if (input != null) input.close();
            if (output != null) output.close();
            if (socket != null) socket.close();
        } catch (Exception ignored) {
        }
    }
}