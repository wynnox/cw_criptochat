package com.project.vaadin;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.messages.MessageListItem;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.VaadinSession;
import elemental.json.Json;
import elemental.json.JsonObject;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@PageTitle("Chat")
@Route(value = "chat-view/:dialogId", layout = MainLayout.class)
public class ChatView extends Composite<VerticalLayout> implements BeforeEnterObserver {

    private static final ZoneId MOSCOW_ZONE = ZoneId.of("Europe/Moscow");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private String username;
    private String dialogId;
    private final MessageList messageList = new MessageList();
    private final List<MessageListItem> messages = new ArrayList<>();

    public ChatView() {
        Object saved = VaadinSession.getCurrent().getAttribute("username");
        username = saved != null ? saved.toString() : "Anonymous";

        MessageInput messageInput = new MessageInput();
        messageInput.setWidthFull();
        messageInput.addSubmitListener(event -> sendMessage(username, event.getValue()));

        VerticalLayout layout = getContent();
        layout.setSizeFull();
        layout.setPadding(true);
        layout.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.STRETCH);

        messageList.setHeight("70vh");
        layout.addAndExpand(messageList);
        layout.add(messageInput);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        dialogId = event.getRouteParameters().get("dialogId").orElse("default");
        connectToWebSocket();
    }

    private void connectToWebSocket() {
        UI.getCurrent().getPage().executeJs("""
            if (!window.chatSocket) {
                window.chatSocket = new WebSocket('ws://' + window.location.host + '/chat/' + $0);
                window.chatSocket.onopen = function() {
                    console.log('✅ WebSocket подключен');
                };
                window.chatSocket.onmessage = function(event) {
                    const message = JSON.parse(event.data);
                    console.log('📨 Получено сообщение', message);
                    $1.$server.receiveMessage(message.user, message.message, message.timestamp);
                };
                window.chatSocket.onclose = function() {
                    console.log('❌ WebSocket закрыт');
                };
            }
        """, dialogId, getElement());
    }

    private void sendMessage(String user, String text) {
        JsonObject msg = Json.createObject();
        msg.put("user", user);
        msg.put("message", text);
        msg.put("timestamp", LocalDateTime.now(MOSCOW_ZONE).format(FORMATTER));

        UI.getCurrent().getPage().executeJs(
                "if (window.chatSocket && window.chatSocket.readyState === WebSocket.OPEN) {" +
                        "window.chatSocket.send($0); }", msg.toJson());
    }

    @ClientCallable
    public void receiveMessage(String user, String text, String timestamp) {
        MessageListItem item = new MessageListItem(text,
                LocalDateTime.parse(timestamp, FORMATTER)
                        .atZone(MOSCOW_ZONE).toInstant(),
                user);
        item.setUserColorIndex(Math.abs(user.hashCode() % 6));
        messages.add(item);
        messageList.setItems(messages);
    }
}
