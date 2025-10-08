package com.project.vaadin;

import com.project.crypto.factories.*;
import com.project.crypto.keyx.DhParams;
import com.project.crypto.keyx.DiffieHellman;
import com.project.crypto.util.Bytes;
import com.project.model.ChatFileMessage;
import com.project.model.Room;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.messages.MessageListItem;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.upload.ProgressUpdateEvent;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinSession;
import elemental.json.Json;
import elemental.json.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Base64;

@PageTitle("Chat")
//@PreserveOnRefresh
//@Push
@Route(value = "chat-view/:dialogId", layout = MainLayout.class)
@Slf4j
public class ChatView extends Composite<VerticalLayout> /*implements BeforeEnterObserver*/ {

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

    private final ProgressBar uploadProgress = new ProgressBar();
    private final Span uploadStatus = new Span();

    private final MessageInput messageInputRef;
    private final Upload uploadRef;

    public ChatView() {
        Object saved = VaadinSession.getCurrent().getAttribute("username");
        username = saved != null ? saved.toString() : "Anonymous";

        // поле ввода текста
        MessageInput messageInput = new MessageInput();
        messageInput.setWidth("60%");
        messageInput.addSubmitListener(event -> sendTextMessage(username, event.getValue()));

        // загрузка файлов
        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        Button uploadBtn = new Button("📎 Прикрепить файл");
        upload.setUploadButton(uploadBtn);

        this.messageInputRef = messageInput;
        this.uploadRef = upload;

        // без блокировки — всё активно сразу
//        messageInput.setEnabled(true);
//        uploadBtn.setEnabled(true);
        messageInput.setEnabled(false);
        uploadBtn.setEnabled(false);

        upload.setMaxFileSize(50 * 1024 * 1024);
        upload.setAcceptedFileTypes("image/*", "application/pdf", "text/plain");
        upload.setDropLabel(new Span("Перетащите сюда или выберите файл"));
        upload.getStyle().set("border", "1px dashed var(--lumo-contrast-30pct)");
        upload.getStyle().set("padding", "8px");
        upload.setWidth("40%");

        // прогресс и статус
        uploadProgress.setWidth("200px");
        uploadProgress.setIndeterminate(false);
        uploadProgress.setVisible(false);
        uploadStatus.getStyle().set("font-size", "var(--lumo-font-size-s)");
        uploadStatus.getStyle().set("color", "var(--lumo-secondary-text-color)");

        upload.addStartedListener(e -> {
            uploadProgress.setValue(0);
            uploadProgress.setVisible(true);
            uploadStatus.setText("Загрузка: " + e.getFileName());
        });

        upload.addProgressListener((ProgressUpdateEvent e) -> {
            if (e.getContentLength() > 0) {
                double progress = (double) e.getReadBytes() / e.getContentLength();
                uploadProgress.setValue(progress);
                uploadStatus.setText("Загрузка: " + (int) (progress * 100) + "%");
            } else {
                uploadStatus.setText("Загрузка...");
            }
        });

        upload.addSucceededListener(e -> {
            onFileUpload(buffer);
            uploadProgress.setVisible(false);
            uploadStatus.setText("Файл загружен: " + e.getFileName());
        });

        upload.addFailedListener(e -> {
            uploadProgress.setVisible(false);
            uploadStatus.setText("Ошибка загрузки: " + e.getFileName());
            Notification.show("Не удалось загрузить файл", 3000, Notification.Position.MIDDLE);
        });

        // кнопки выхода
        Button leaveButton = new Button("🚪 Выйти из чата", e -> disconnectUser());
        leaveButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button closeButton = new Button("❌ Закрыть комнату", e -> confirmCloseChat());
        closeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);

        HorizontalLayout topControls = new HorizontalLayout(messageInput, upload);
        topControls.setWidthFull();
        topControls.setAlignItems(FlexComponent.Alignment.CENTER);

        HorizontalLayout uploadInfo = new HorizontalLayout(uploadProgress, uploadStatus);
        uploadInfo.setAlignItems(FlexComponent.Alignment.CENTER);

        HorizontalLayout bottomControls = new HorizontalLayout(leaveButton, closeButton);
        bottomControls.setWidthFull();
        bottomControls.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        bottomControls.getStyle().set("margin-top", "10px");

        VerticalLayout layout = getContent();
        layout.setSizeFull();
        layout.setPadding(true);
        layout.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.STRETCH);
        messageList.setHeight("70vh");

        layout.addAndExpand(messageList);
        layout.add(topControls, uploadInfo, bottomControls);
    }

    // ---------- Загрузка файла ----------
    private void onFileUpload(MemoryBuffer buffer) {
        String fileName = buffer.getFileData().getFileName();
        String mimeType = buffer.getFileData() != null
                ? buffer.getFileData().getMimeType()
                : "application/octet-stream";

        try (InputStream inputStream = buffer.getInputStream()) {
            byte[] data = inputStream.readAllBytes();

            ChatFileMessage message = new ChatFileMessage(data, fileName, mimeType,
                    mimeType.startsWith("image") ? "IMAGE" : "FILE");
            sendEncrypted(username, message.getData(), message.getFileName(), message.getMimeType());

            MessageListItem msgItem = new MessageListItem(
                    (mimeType.startsWith("image") ? "🖼 " : "📎 Файл: ") + fileName,
                    LocalDateTime.now(MOSCOW_ZONE).atZone(MOSCOW_ZONE).toInstant(),
                    username
            );
            msgItem.setUserColorIndex(Math.abs(username.hashCode() % 6));
            messages.add(msgItem);
            messageList.setItems(messages);

            // превью изображения под сообщением
            if (mimeType.startsWith("image")) {
                StreamResource res = new StreamResource(fileName, () -> new ByteArrayInputStream(data));
                Image img = new Image(res, fileName);
                img.setMaxWidth("200px");
                img.getStyle().set("border-radius", "8px").set("margin", "6px 0 12px 48px");
                getContent().add(img);
            }

            messageList.getElement().executeJs("this.scrollToIndex($0)", messages.size() - 1);
            Notification.show("Файл отправлен: " + fileName);
        } catch (IOException ex) {
            Notification.show("Ошибка при чтении файла: " + ex.getMessage());
        }
    }

    // ---------- Ключи и подключение ----------
//    @Override
//    public void beforeEnter(BeforeEnterEvent event) {
//        dialogId = event.getRouteParameters().get("dialogId").orElse("default");
//        VaadinSession sess = VaadinSession.getCurrent();
//        algorithm = (String) sess.getAttribute("algorithm");
//        mode = (String) sess.getAttribute("mode");
//        padding = (String) sess.getAttribute("padding");
//
//        userId = username + "-" + UUID.randomUUID();
//        performKeyExchange();
//    }
    @Override
    protected void onAttach(AttachEvent event) {
        log.info("onAttach for user {}", username);

        super.onAttach(event);

        // Получаем текущую локацию (включая параметры маршрута)
        Location location = UI.getCurrent().getInternals().getActiveViewLocation();

        // Извлекаем сегменты пути
        List<String> segments = location.getSegments();
        // Например, при URL: /chat-view/123 → segments = ["chat-view", "123"]

        dialogId = segments.size() > 1 ? segments.get(1) : "default";

        VaadinSession sess = VaadinSession.getCurrent();
        algorithm = (String) sess.getAttribute("algorithm");
        mode = (String) sess.getAttribute("mode");
        padding = (String) sess.getAttribute("padding");
        userId = username + "-" + UUID.randomUUID();
        performKeyExchange();
    }

    @SuppressWarnings("unchecked")
    private void performKeyExchange() {
        try {
            log.info("performing key exchange for user {}", username);
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

            DhParams params = new DhParams(p, q, g);
            dh = new DiffieHellman(params);
            privateKey = dh.generatePrivate();
            byte[] publicKey = dh.derivePublic(privateKey);

            String submitUrl = "http://localhost:8080/room/" + dialogId
                    + "/submitKey?userId=" + userId
                    + "&publicKey=" + new BigInteger(1, publicKey);
            rest.postForObject(submitUrl, null, String.class);

            UI ui = UI.getCurrent();
            keyPoller = new Timer(true);
            keyPoller.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        log.info("polling keys for user {}", username);
                        Map<String, Object> keyMap = rest.getForObject(
                                "http://localhost:8080/room/" + dialogId + "/keys",
                                Map.class);
                        if (keyMap != null && keyMap.size() >= 2) {
                            log.info("keys polled for user {}", username);
                            for (Map.Entry<String, Object> e : keyMap.entrySet()) {
                                if (!e.getKey().equals(userId)) {
                                    BigInteger peerY = new BigInteger(e.getValue().toString());
                                    sharedKey = dh.deriveShared(privateKey, Bytes.toFixed(peerY, dh.getEncodedLength()));
                                    ui.access(ChatView.this::setupCryptoSuite);
                                    this.cancel();
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }, 1000, 2000);
        } catch (Exception e) {
            Notification.show("Ошибка при обмене ключами: " + e.getMessage());
        }
    }

    private void setupCryptoSuite() {
        try {
            log.info("setting up suite for user {}", username);
            if (keyPoller != null) keyPoller.cancel();

            suite = new CryptoFactory.Builder()
                    .algorithm(AlgorithmType.MARS)
                    .mode(ModeType.CBC)
                    .padding(PaddingType.PKCS7)
                    .key(Arrays.copyOf(sharedKey, 16))
                    .buildSuite();

            cryptoReady = true;
            UI.getCurrent().access(() -> {
                messageInputRef.getElement().setEnabled(true);
                uploadRef.getUploadButton().getElement().setEnabled(true);
            });
            log.info("everything should be unlocked now for user {} session {}", username, UI.getCurrent().getSession().getSession().getId());
            connectToWebSocket();
            Notification.show("Ключи обменяны. Соединение защищено.");
        } catch (Exception e) {
            Notification.show("Ошибка инициализации шифра: " + e.getMessage());
        }
    }

    // ---------- WebSocket ----------
    private void connectToWebSocket() {
        log.info("connecting to websocket for user {}, session {}", username, UI.getCurrent().getSession().getSession().getId());
        UI.getCurrent().getPage().executeJs("""
            if (window.chatSocket &&
                window.chatSocket.readyState !== WebSocket.CLOSED &&
                window.chatSocket.readyState !== WebSocket.CLOSING) {
                window.chatSocket._manualClose = true;
                window.chatSocket.close();
            }
            function connectChatSocket(roomId, element, userId, username) {
                const proto = (location.protocol === 'https:') ? 'wss://' : 'ws://';
                const ws = new WebSocket(proto + window.location.host + '/chat/' + roomId);
                ws.onopen = () => ws.send(JSON.stringify({ type: "join", userId, user: username }));
                ws.onmessage = event => {
                    const m = JSON.parse(event.data);
                    if (!m) return;
                    if (["join", "leave", "closed", "system"].includes(m.type)) {
                        element.$server.handleUserEvent(m.type, m.user);
                        return;
                    }
                    if (m.userId === userId) return;
                    if (m.ciphertext && m.iv) {
                        element.$server.receiveEncrypted(
                            m.user, m.iv, m.ciphertext, m.timestamp,
                            m.fileName || null, m.mimeType || null
                        );
                    }
                };
                window.chatSocket = ws;
            }
            connectChatSocket($0, $1, $2, $3);
        """, dialogId, getElement(), userId, username);
    }

    @ClientCallable
    public void handleUserEvent(String type, String user) {
        if ("closed".equals(type)) {
            log.info("Комната {} закрыта по инициативе {}", dialogId, user);
            Notification.show("Комната закрыта");
            getUI().ifPresent(ui -> ui.navigate("")); // Возврат на главную
        }
        else if ("join".equals(type)) {
            Notification.show(user + " подключился к чату");
        }
        else if ("leave".equals(type)) {
            Notification.show(user + " покинул чат");
        }
    }


    // ---------- Отправка / получение сообщений ----------
    private void sendTextMessage(String user, String text) {
        sendEncrypted(user, text.getBytes(StandardCharsets.UTF_8), null, null);
        addUiMessage(user, text, LocalDateTime.now(MOSCOW_ZONE).format(FORMATTER));
    }

    private void sendEncrypted(String user, byte[] data, String fileName, String mimeType) {
        try {
            byte[] iv = new byte[suite.getBlockSize()];
            rng.nextBytes(iv);
            byte[] ciphertext = suite.encrypt(data, iv);

            JsonObject msg = Json.createObject();
            msg.put("type", "message");
            msg.put("userId", userId);
            msg.put("user", user);
            msg.put("iv", Base64.getEncoder().encodeToString(iv));
            msg.put("ciphertext", Base64.getEncoder().encodeToString(ciphertext));
            msg.put("timestamp", LocalDateTime.now(MOSCOW_ZONE).format(FORMATTER));
            if (fileName != null) msg.put("fileName", fileName);
            if (mimeType != null) msg.put("mimeType", mimeType);

            UI.getCurrent().getPage().executeJs("""
                const payload = $0;
                if (window.chatSocket && window.chatSocket.readyState === WebSocket.OPEN)
                    window.chatSocket.send(payload);
            """, msg.toJson());
        } catch (Exception e) {
            Notification.show("Ошибка шифрования: " + e.getMessage());
        }
    }

    @ClientCallable
    public void receiveEncrypted(String user, String ivBase64, String cipherBase64,
                                 String timestamp, String fileName, String mimeType) {
        try {
            byte[] iv = Base64.getDecoder().decode(ivBase64);
            byte[] cipher = Base64.getDecoder().decode(cipherBase64);
            byte[] plaintext = suite.decrypt(cipher, iv);

            if (fileName != null && mimeType != null) {
                MessageListItem item = new MessageListItem(
                        (mimeType.startsWith("image") ? "🖼 " : "📎 Файл: ") + fileName,
                        LocalDateTime.parse(timestamp, FORMATTER).atZone(MOSCOW_ZONE).toInstant(),
                        user
                );
                item.setUserColorIndex(Math.abs(user.hashCode() % 6));
                messages.add(item);
                messageList.setItems(messages);

                StreamResource res = new StreamResource(fileName, () -> new ByteArrayInputStream(plaintext));

                if (mimeType.startsWith("image")) {
                    Image img = new Image(res, fileName);
                    img.setMaxWidth("200px");
                    img.getStyle().set("border-radius", "8px").set("margin", "6px 0 12px 48px");
                    getContent().add(img);
                } else {
                    Anchor downloadLink = new Anchor(res, "⬇ Скачать " + fileName);
                    downloadLink.getElement().setAttribute("download", true); // важно!
                    downloadLink.getStyle()
                            .set("margin-left", "48px")
                            .set("margin-bottom", "12px")
                            .set("display", "block");
                    getContent().add(downloadLink);
                }

                messageList.getElement().executeJs("this.scrollToIndex($0)", messages.size() - 1);
            } else {
                String text = new String(plaintext, StandardCharsets.UTF_8);
                addUiMessage(user, text, timestamp);
            }
        } catch (Exception e) {
            Notification.show("Ошибка дешифрования: " + e.getMessage());
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
        messageList.getElement().executeJs("this.scrollToIndex($0)", messages.size() - 1);
    }

    // ---------- Выход и закрытие ----------
    private void confirmCloseChat() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Закрыть комнату?");
        dialog.add(new Span("После закрытия оба участника будут возвращены в главное меню."));
        Button yes = new Button("Да, закрыть", e -> {
            dialog.close();
            broadcastCloseEvent();
        });
        Button cancel = new Button("Отмена", e -> dialog.close());
        dialog.getFooter().add(yes, cancel);
        dialog.open();
    }

    private void broadcastCloseEvent() {
        UI.getCurrent().getPage().executeJs("""
            if (window.chatSocket && window.chatSocket.readyState === WebSocket.OPEN) {
                window.chatSocket._manualClose = true;
                window.chatSocket.send(JSON.stringify({ type: "closed", userId: $0 }));
                window.chatSocket.close();
            }
        """, userId);
        getUI().ifPresent(ui -> ui.navigate(""));
    }

    private void disconnectUser() {
        try {
            if (keyPoller != null) keyPoller.cancel();
            UI.getCurrent().getPage().executeJs("""
                if (window.chatSocket && window.chatSocket.readyState === WebSocket.OPEN) {
                    window.chatSocket._manualClose = true;
                    window.chatSocket.send(JSON.stringify({ type: "leave", userId: $0 }));
                                        window.chatSocket.close();
                }
            """, userId);
            getUI().ifPresent(ui -> ui.navigate(""));
        } catch (Exception e) {
            Notification.show("Ошибка при отключении: " + e.getMessage());
        }
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (keyPoller != null) keyPoller.cancel();
        UI.getCurrent().getPage().executeJs("""
            if (window.chatSocket && window.chatSocket.readyState === WebSocket.OPEN) {
                window.chatSocket._manualClose = true;
                window.chatSocket.close();
            }
        """);
        super.onDetach(detachEvent);
    }
}
