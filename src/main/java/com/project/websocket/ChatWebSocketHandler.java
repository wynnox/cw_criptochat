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

    // активные комнаты: roomId → набор сессий
    private static final Map<String, Set<WebSocketSession>> rooms = new ConcurrentHashMap<>();

    // история сообщений по комнатам
    private static final Map<String, List<String>> messageHistory = new ConcurrentHashMap<>();

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final ZoneId MOSCOW_ZONE = ZoneId.of("Europe/Moscow");

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String path = session.getUri() != null ? session.getUri().getPath() : "";
        String roomId = extractRoomId(path);
        if (roomId != null) {
            rooms.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(session);
            log.info("Новое подключение в комнату {} ({} активных)", roomId, rooms.get(roomId).size());
        }
    }

    private void removeSession(String roomId, WebSocketSession session) {
        Set<WebSocketSession> sessions = rooms.get(roomId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                rooms.remove(roomId);
                messageHistory.remove(roomId);
                log.info("Комната {} опустела и была очищена", roomId);
            }
        }
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        String payload = message.getPayload();
        log.info("Получено: {}", payload);

        try {
            JsonObject json = Json.parse(payload);
            json.put("timestamp", LocalDateTime.now(MOSCOW_ZONE).format(FORMATTER));

            String path = session.getUri() != null ? session.getUri().getPath() : "";
            String roomId = extractRoomId(path);
            if (roomId == null) return;

            String type = json.hasKey("type") ? json.getString("type") : "message";
            String user = json.hasKey("user") ? json.getString("user") : "unknown";
            String userId = json.hasKey("userId") ? json.getString("userId") : "";

            switch (type) {
                case "join" -> {
                    rooms.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(session);
                    broadcast(roomId, makeEvent("join", user));
                    log.info("{} подключился к комнате {}", user, roomId);
                }
                case "leave" -> {
                    broadcast(roomId, makeEvent("leave", user));
                    removeSession(roomId, session);
                    messageHistory.remove(roomId); // очистить историю, если пользователь вышел
                    log.info("{} покинул комнату {}", user, roomId);
                }
                case "closed" -> {
                    //broadcast(roomId, makeEvent("closed", user));
                    //closeRoom(roomId);
                    //log.info("Комната {} закрыта пользователем {}", roomId, user);
                    // Сначала уведомляем всех участников
                    String msg = makeEvent("closed", user);
                    broadcast(roomId, msg);
                    log.info("Комната {} закрывается пользователем {}", roomId, user);

                    // Даём клиентам секунду на обработку события
                    new Timer(true).schedule(new TimerTask() {
                        @Override
                        public void run() {
                            closeRoom(roomId);
                            log.info("Комната {} полностью закрыта", roomId);
                        }
                    }, 1000);
                }
                case "message" -> {
                    Set<WebSocketSession> roomSessions = rooms.get(roomId);
                    if (roomSessions == null || roomSessions.size() < 2) {
                        log.warn("Комната {}: недостаточно клиентов для рассылки", roomId);
                        return;
                    }

                    messageHistory.computeIfAbsent(roomId,
                            id -> Collections.synchronizedList(new ArrayList<>())
                    ).add(json.toJson());
                    broadcast(roomId, json.toJson());
                }
                default -> log.warn("Неизвестный тип: {}", type);
            }
        } catch (Exception e) {
            log.error("Ошибка обработки сообщения: {}", e.getMessage(), e);
        }
    }


    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String path = session.getUri() != null ? session.getUri().getPath() : "";
        String roomId = extractRoomId(path);
        if (roomId != null) {
            Set<WebSocketSession> sessions = rooms.get(roomId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    rooms.remove(roomId);
                    log.info("Комната {} опустела и удалена", roomId);
                }
            }
        }
        log.info("WebSocket закрыт: {}", session.getId());
    }

    private String extractRoomId(String path) {
        if (path != null && path.startsWith("/chat/")) {
            return path.substring("/chat/".length());
        }
        return null;
    }

    private void broadcast(String roomId, String json) {
        Set<WebSocketSession> sessions = rooms.get(roomId);
        if (sessions == null) return;

        synchronized (sessions) {
            for (WebSocketSession s : sessions) {
                if (!s.isOpen()) continue;
                try {
                    s.sendMessage(new TextMessage(json));
                } catch (Exception e) {
                    log.error("Ошибка отправки WS: {}", e.getMessage());
                }
            }
        }
    }

    private String makeEvent(String type, String user) {
        JsonObject event = Json.createObject();
        event.put("type", type);
        event.put("user", user);
        event.put("timestamp", LocalDateTime.now(MOSCOW_ZONE).format(FORMATTER));
        return event.toJson();
    }

    private void closeRoom(String roomId) {
        Set<WebSocketSession> sessions = rooms.remove(roomId);
        if (sessions != null) {
            for (WebSocketSession s : sessions) {
                try {
                    if (s.isOpen()) s.close();
                } catch (Exception e) {
                    log.error("Ошибка при закрытии сессии: {}", e.getMessage());
                }
            }
        }
        messageHistory.remove(roomId);
        log.info("Комната {} полностью очищена", roomId);
    }


    public static List<String> getHistory(String roomId) {
        return messageHistory.getOrDefault(roomId, List.of());
    }

    public static void clearHistory(String roomId) {
        messageHistory.remove(roomId);
    }

    // вспомогательная отправка Vaadin-сессии, если нужно вызвать UI
    public static void sendToSession(String sessionId, String jsonMessage) {
        VaadinSession session = SessionRegistry.get(sessionId);
        if (session != null) {
            session.access(() -> {
                try {
                    UI ui = session.getUIs().stream().findFirst().orElse(null);
                    if (ui != null) {
                        ui.getPage().executeJs(
                                "if ($0 && $0.$server && $0.$server.receiveMessage) " +
                                        "$0.$server.receiveMessage('system', $1, new Date().toISOString());",
                                ui.getElement(), jsonMessage
                        );
                    }
                } catch (Exception e) {
                    log.error("Ошибка отправки в Vaadin UI {}: {}", sessionId, e.getMessage());
                }
            });
        } else {
            log.warn("Vaadin-сессия {} не найдена", sessionId);
        }
    }
}
