package com.wxrley.client;

import com.wxrley.model.Message;
import com.wxrley.server.Server;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

/**
 * Célula customizada para renderizar mensagens no ListView do chat.
 * Diferencia visualmente mensagens do sistema, do usuário e de outros participantes.
 * Utilizada por ChatController via setCellFactory.
 */
public class MessageCell extends ListCell<Message> {

    private final String userName;
    private final HBox container = new HBox();
    private final VBox bubble = new VBox(2);
    private final TextFlow messageFlow = new TextFlow();
    private final HBox timeContainer = new HBox();
    private final Text timeText = new Text();

    /**
     * Inicializa os componentes visuais da célula.
     * O userName é usado para identificar mensagens do próprio usuário.
     */
    public MessageCell(String userName) {
        this.userName = userName;

        container.setPadding(new Insets(5));
        bubble.setPadding(new Insets(8));
        bubble.setMaxWidth(250);

        timeText.getStyleClass().add("text-time");
        timeContainer.getChildren().add(timeText);

        bubble.getChildren().addAll(messageFlow, timeContainer);
        container.getChildren().add(bubble);
    }

    /**
     * Renderiza a mensagem de acordo com o tipo de remetente.
     * Limpa estilos anteriores e aplica classe CSS apropriada (bubble-system, bubble-user, bubble-other).
     * Chamado automaticamente pelo JavaFX ao atualizar itens do ListView.
     */
    @Override
    protected void updateItem(Message msg, boolean empty) {
        super.updateItem(msg, empty);

        if (empty || msg == null) {
            setGraphic(null);
            return;
        }

        messageFlow.getChildren().clear();
        bubble.getStyleClass().removeAll("bubble-system", "bubble-user", "bubble-other");

        String senderName = msg.getSender().name();
        String content = msg.getContent();
        timeText.setText(msg.getTimestamp());

        if (Server.SYSTEM_USER.name().equals(senderName)) {
            renderSystemMessage(content);
        } else if (senderName.equals(userName)) {
            renderUserMessage(content);
        } else {
            renderOtherMessage(senderName, content);
        }

        setGraphic(container);
    }

    /**
     * Renderiza mensagens do sistema centralizadas.
     * Se a mensagem contém o nome do usuário, destaca em cor diferente.
     * Aplica classe CSS "bubble-system".
     */
    private void renderSystemMessage(String content) {
        bubble.getStyleClass().add("bubble-system");
        timeContainer.setAlignment(Pos.CENTER);
        bubble.setAlignment(Pos.CENTER);
        container.setAlignment(Pos.CENTER);

        if (content.contains(userName)) {
            int nameIndex = content.indexOf(userName);

            String before = content.substring(0, nameIndex);
            String after = content.substring(nameIndex + userName.length());

            addText(before, "text-system");
            addText(userName, "text-name-system");
            addText(after, "text-system");
        } else {
            addText(content, "text-system");
        }
    }

    /**
     * Renderiza mensagens do próprio usuário alinhadas à direita.
     * Aplica classe CSS "bubble-user".
     */
    private void renderUserMessage(String content) {
        bubble.getStyleClass().add("bubble-user");
        timeContainer.setAlignment(Pos.BOTTOM_RIGHT);
        bubble.setAlignment(Pos.TOP_LEFT);
        container.setAlignment(Pos.CENTER_RIGHT);

        addText(content, "text-user");
    }

    /**
     * Renderiza mensagens de outros usuários alinhadas à esquerda.
     * Inclui o nome do remetente antes do conteúdo.
     * Aplica classe CSS "bubble-other".
     */
    private void renderOtherMessage(String name, String content) {
        bubble.getStyleClass().add("bubble-other");
        timeContainer.setAlignment(Pos.BOTTOM_RIGHT);
        bubble.setAlignment(Pos.TOP_LEFT);
        container.setAlignment(Pos.CENTER_LEFT);

        addText(name + ": ", "text-name-message");
        addText(content, "text-other");
    }

    /**
     * Adiciona texto com estilo CSS específico ao TextFlow.
     */
    private void addText(String content, String styleClass) {
        Text text = new Text(content);
        text.getStyleClass().add(styleClass);
        messageFlow.getChildren().add(text);
    }
}