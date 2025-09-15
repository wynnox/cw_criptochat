package com.project.vaadin;

import com.project.crypto.factories.*;
import com.project.crypto.keyx.DhParams;
import com.project.crypto.keyx.DiffieHellman;
import com.project.crypto.util.Bytes;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.messages.MessageListItem;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.VaadinSession;
import elemental.json.Json;
import elemental.json.JsonObject;
import org.springframework.web.client.RestTemplate;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@PageTitle("Chat")
@PreserveOnRefresh
@Route(value = "chat-view/:dialogId", layout = MainLayout.class)
public class ChatView extends Composite<VerticalLayout> implements BeforeEnterObserver {

    private static final ZoneId MOSCOW_ZONE = ZoneId.of("Europe/Moscow");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final String username;
    private String dialogId;
    private String userId;
    private final MessageList messageList = new MessageList();
    private final List<MessageListItem> messages = new ArrayList<>();
    private final RestTemplate rest = new RestTemplate();

    private DiffieHellman dh;
    private byte[] privateKey;
    private byte[] sharedKey;

    private CryptoSuite suite;
    private final SecureRandom rng = new SecureRandom();

    private String algorithm;
    private String mode;
    private String padding;

    private Timer keyPoller;
    private final List<Runnable> pendingDecrypt = new ArrayList<>();
    private volatile boolean cryptoReady = false;

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

        VaadinSession sess = VaadinSession.getCurrent();

        algorithm = (String) sess.getAttribute("algorithm");
        mode = (String) sess.getAttribute("mode");
        padding = (String) sess.getAttribute("padding");

        if (sess.getAttribute("userId") == null) {
            sess.setAttribute("userId", username + "-" + UUID.randomUUID());
        }
        userId = sess.getAttribute("userId").toString();

        Object savedKey = sess.getAttribute("sharedKey");
        Object savedSuite = sess.getAttribute("cryptoSuite");

        if (savedKey != null && savedSuite != null) {
            sharedKey = (byte[]) savedKey;
            suite = (CryptoSuite) savedSuite;
            cryptoReady = true;
            Notification.show("Соединение восстановлено");
            connectToWebSocket();
        } else {
            performKeyExchange();
        }
    }


    @SuppressWarnings("unchecked")
    private void performKeyExchange() {
        try {
            String url = "http://localhost:8080/room/list";
            List<Map<String, Object>> rooms =
                    Arrays.asList(Objects.requireNonNull(rest.getForObject(url, Map[].class)));

            Map<String, Object> myRoom = rooms.stream()
                    .filter(r -> dialogId.equals(r.get("id")))
                    .findFirst()
                    .orElseThrow();

            BigInteger p = new BigInteger(myRoom.get("p").toString());
            BigInteger q = new BigInteger(myRoom.get("q").toString());
            BigInteger g = new BigInteger(myRoom.get("g").toString());

            algorithm = Objects.toString(myRoom.get("algorithm"), algorithm);
            mode = Objects.toString(myRoom.get("mode"), mode);
            padding = Objects.toString(myRoom.get("padding"), padding);

            DhParams params = new DhParams(p, q, g);
            dh = new DiffieHellman(params);

            privateKey = dh.generatePrivate();
            byte[] publicKey = dh.derivePublic(privateKey);

            String submitUrl = "http://localhost:8080/room/" + dialogId
                    + "/submitKey?userId=" + userId
                    + "&publicKey=" + new BigInteger(1, publicKey);
            rest.postForObject(submitUrl, null, String.class);

            Notification.show("Ключ отправлен. Ожидаем второго участника...");

            UI ui = UI.getCurrent();
            keyPoller = new Timer(true);
            keyPoller.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        Map<String, Object> keyMap = rest.getForObject(
                                "http://localhost:8080/room/" + dialogId + "/keys",
                                Map.class);

                        if (keyMap != null && keyMap.size() >= 2) {
                            for (Map.Entry<String, Object> e : keyMap.entrySet()) {
                                if (!e.getKey().equals(userId)) {
                                    BigInteger peerY = new BigInteger(e.getValue().toString());
                                    sharedKey = dh.deriveShared(
                                            privateKey,
                                            Bytes.toFixed(peerY, dh.getEncodedLength())
                                    );
                                    ui.access(() -> {
                                        Notification.show("Ключи обменяны. Соединение защищено.");
                                        setupCryptoSuite();
                                        connectToWebSocket();
                                    });
                                    this.cancel();
                                    return;
                                }
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }
            }, 1000, 2000);

        } catch (Exception e) {
            Notification.show("Ошибка при обмене ключами: " + e.getMessage());
        }
    }

    private void setupCryptoSuite() {
        try {
            if (keyPoller != null) {
                keyPoller.cancel();
                keyPoller.purge();
            }

            suite = new CryptoFactory.Builder()
                    .algorithm(AlgorithmType.valueOf(algorithm))
                    .mode(ModeType.valueOf(mode))
                    .padding(PaddingType.valueOf(padding))
                    .key(Arrays.copyOf(sharedKey, 16))
                    .buildSuite();

            cryptoReady = true;
            for (Runnable r : new ArrayList<>(pendingDecrypt)) r.run();
            pendingDecrypt.clear();

            VaadinSession sess = VaadinSession.getCurrent();
            sess.setAttribute("sharedKey", sharedKey);
            sess.setAttribute("cryptoSuite", suite);

            Notification.show("Используется: " + algorithm + " / " + mode + " / " + padding);
        } catch (Exception e) {
            Notification.show("Ошибка инициализации шифра: " + e.getMessage());
        }
    }

    private void connectToWebSocket() {
        UI.getCurrent().getPage().executeJs("""
            function connectChatSocket(roomId, element, userId) {
                const proto = (location.protocol === 'https:') ? 'wss://' : 'ws://';
                const ws = new WebSocket(proto + window.location.host + '/chat/' + roomId);
                ws._queue = [];

                ws.onopen = () => {
                    for (const m of ws._queue) ws.send(m);
                    ws._queue = [];
                };

                ws.onmessage = event => {
                    const m = JSON.parse(event.data);
                    if (m && m.userId === userId) return; // игнорируем эхо
                    if (m && m.ciphertext && m.iv) {
                        element.$server.receiveEncrypted(m.user, m.iv, m.ciphertext, m.timestamp);
                    }
                };

                ws.onclose = () => {
                    console.warn('WebSocket закрыт, переподключение...');
                    setTimeout(() => connectChatSocket(roomId, element, userId), 1500);
                };

                window.chatSocket = ws;
            }
            connectChatSocket($0, $1, $2);
        """, dialogId, getElement(), userId);
    }

    private void sendMessage(String user, String text) {
        if (suite == null) {
            Notification.show("Ключ не установлен — шифрование невозможно");
            return;
        }

        try {
            byte[] iv = new byte[suite.getBlockSize()];
            rng.nextBytes(iv);
            byte[] ciphertext = suite.encrypt(text.getBytes(StandardCharsets.UTF_8), iv);

            addUiMessage(user, text, LocalDateTime.now(MOSCOW_ZONE).format(FORMATTER));

            JsonObject msg = Json.createObject();
            msg.put("userId", userId);
            msg.put("user", user);
            msg.put("iv", Base64.getEncoder().encodeToString(iv));
            msg.put("ciphertext", Base64.getEncoder().encodeToString(ciphertext));
            msg.put("timestamp", LocalDateTime.now(MOSCOW_ZONE).format(FORMATTER));

            UI.getCurrent().getPage().executeJs("""
                if (window.chatSocket) {
                    const payload = $0;
                    if (window.chatSocket.readyState === WebSocket.OPEN) {
                        window.chatSocket.send(payload);
                    } else {
                        if (!window.chatSocket._queue) window.chatSocket._queue = [];
                        window.chatSocket._queue.push(payload);
                    }
                }
            """, msg.toJson());
        } catch (Exception e) {
            Notification.show("Ошибка шифрования: " + e.getMessage());
        }
    }

    @ClientCallable
    public void receiveEncrypted(String user, String ivBase64, String cipherBase64, String timestamp) {
        Runnable task = () -> {
            try {
                byte[] iv = Base64.getDecoder().decode(ivBase64);
                byte[] cipher = Base64.getDecoder().decode(cipherBase64);
                byte[] plaintext = suite.decrypt(cipher, iv);
                String text = new String(plaintext, StandardCharsets.UTF_8);
                addUiMessage(user, text, timestamp);
            } catch (Exception e) {
                Notification.show("Ошибка дешифрования: " + e.getMessage());
            }
        };
        if (!cryptoReady || suite == null) {
            pendingDecrypt.add(task);
        } else {
            task.run();
        }
    }

    private void addUiMessage(String user, String text, String timestamp) {
        MessageListItem item = new MessageListItem(
                text,
                LocalDateTime.parse(timestamp, FORMATTER).atZone(MOSCOW_ZONE).toInstant(),
                user
        );
        item.setUserColorIndex(Math.abs(user.hashCode() % 6));
        messages.add(item);
        messageList.setItems(messages);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (keyPoller != null) keyPoller.cancel();
        super.onDetach(detachEvent);
    }
}
