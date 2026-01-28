package com.wxrley.model;

import java.io.Serializable;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Representa uma mensagem do chat que pode ser transmitida via socket.
 * Contém remetente, conteúdo, timestamp gerado automaticamente e flag de reconexão.
 * Utilizada por Connection, Server e ClientHandler para troca de mensagens.
 */
public class Message implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final User sender;
    private final String content;
    private final String timestamp;
    private final boolean isReconnection;

    public Message(User sender, String content) {
        this(sender, content, false);
    }

    public Message(User sender, String content, boolean isReconnection) {
        this.sender = sender;
        this.content = content;
        this.timestamp = LocalTime.now().format(TIME_FORMATTER);
        this.isReconnection = isReconnection;
    }

    public User getSender() {
        return sender;
    }

    public String getContent() {
        return content;
    }

    public String getTimestamp() {
        return timestamp;
    }
    public boolean isReconnection() {
        return isReconnection;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s: %s", timestamp, sender.name(), content);
    }
}