package com.project.websocket;

import com.project.vaadin.SessionRegistry;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinSession;
import elemental.json.Json;
import elemental.json.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Set<WebSocketSession> sessions = Collections.synchronizedSet(new HashSet<>());

    private static final Map<String, List<String>> messageHistory = new ConcurrentHashMap<>();

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final ZoneId MOSCOW_ZONE = ZoneId.of("Europe/Moscow");

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        log.info("Новое WebSocket-соединение: {}", session.getId());

        String path = session.getUri() != null ? session.getUri().getPath() : "";
        String roomId = extractRoomId(path);

        if (roomId != null && messageHistory.containsKey(roomId)) {
            for (String msg : messageHistory.get(roomId)) {
                session.sendMessage(new TextMessage(msg));
            }
            log.info("История ({} сообщений) отправлена в комнату {}", messageHistory.get(roomId).size(), roomId);
        }
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.info("Получено сообщение: {}", payload);

        try {
            JsonObject json = Json.parse(payload);
            json.put("timestamp", LocalDateTime.now(MOSCOW_ZONE).format(FORMATTER));

            String path = session.getUri() != null ? session.getUri().getPath() : "";
            String roomId = extractRoomId(path);

            if (roomId != null) {
                messageHistory.computeIfAbsent(roomId, id -> Collections.synchronizedList(new ArrayList<>()))
                        .add(json.toJson());
            }

            synchronized (sessions) {
                for (WebSocketSession ws : sessions) {
                    if (ws.isOpen()) {
                        ws.sendMessage(new TextMessage(json.toJson()));
                        log.info("Отправлено: {} -> {}", session.getId(), ws.getId());
                    }
                }
            }

            synchronized (sessions) {
                for (WebSocketSession ws : sessions) {
                    if (ws.isOpen()) {
                        ws.sendMessage(new TextMessage(json.toJson()));
                        log.info("Отправлено: {} -> {}", session.getId(), ws.getId());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Ошибка при обработке сообщения: {}", e.getMessage(), e);
        }
    }

    private String extractRoomId(String path) {
        if (path != null && path.startsWith("/chat/")) {
            return path.substring("/chat/".length());
        }
        return null;
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        log.info("Соединение закрыто: {}", session.getId());
    }

    public static void sendToSession(String sessionId, String jsonMessage) {
        VaadinSession session = SessionRegistry.get(sessionId);
        if (session != null) {
            session.access(() -> {
                try {
                    UI ui = session.getUIs().stream().findFirst().orElse(null);
                    if (ui != null) {
                        // вызов серверного метода receiveMessage прямо в UI
                        ui.getPage().executeJs(
                                "if ($0 && $0.$server && $0.$server.receiveMessage) " +
                                        "$0.$server.receiveMessage('system', $1, new Date().toISOString());",
                                ui.getElement(), jsonMessage
                        );
                    }
                } catch (Exception e) {
                    log.error("Ошибка отправки в сессию {}: {}", sessionId, e.getMessage(), e);
                }
            });
        } else {
            log.warn("Сессия {} не найдена (возможно, закрыта)", sessionId);
        }
    }

    public static List<String> getHistory(String roomId) {
        return messageHistory.getOrDefault(roomId, List.of());
    }

    public static void clearHistory(String roomId) {
        messageHistory.remove(roomId);
    }

}
