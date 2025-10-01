package com.project.vaadin;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@PageTitle("CryptoChat — вход")
@Route(value = "")
public class HomeView extends VerticalLayout {

    private final RestTemplate rest = new RestTemplate();

    public HomeView() {
        setDefaultHorizontalComponentAlignment(Alignment.CENTER);
        setSpacing(true);
        setPadding(true);

        // поле имени пользователя
        TextField usernameField = new TextField("Имя пользователя");
        usernameField.setPlaceholder("Введите имя...");
        usernameField.setWidth("300px");

        // поле id комнаты
        TextField roomField = new TextField("ID комнаты");
        roomField.setPlaceholder("Введите ID комнаты для подключения...");
        roomField.setWidth("300px");

        // кнопки
        Button joinButton = new Button("Подключиться", e -> {
            String name = usernameField.getValue().trim();
            String roomId = roomField.getValue().trim();

            if (name.isEmpty()) {
                Notification.show("Введите имя пользователя");
                return;
            }

            VaadinSession.getCurrent().setAttribute("username", name);

            if (!roomId.isEmpty()) {
                getUI().ifPresent(ui -> ui.navigate("chat-view/" + roomId));
            } else {
                Notification.show("Введите ID комнаты или создайте новую");
            }
        });

        Button createButton = new Button("Создать комнату", e -> openCreateDialog(usernameField));

        HorizontalLayout buttons = new HorizontalLayout(joinButton, createButton);
        buttons.setSpacing(true);

        add(usernameField, roomField, buttons);
    }

    private void openCreateDialog(TextField usernameField) {
        if (usernameField.getValue().trim().isEmpty()) {
            Notification.show("Сначала введите имя пользователя");
            return;
        }

        VaadinSession.getCurrent().setAttribute("username", usernameField.getValue().trim());

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Создание комнаты");

        Select<String> algorithmSelect = new Select<>();
        algorithmSelect.setLabel("Алгоритм");
        algorithmSelect.setItems("MARS", "MAGENTA");
        algorithmSelect.setPlaceholder("Выберите алгоритм");

        Select<String> modeSelect = new Select<>();
        modeSelect.setLabel("Режим шифрования");
        modeSelect.setItems("CBC", "PCBC", "ECB", "CTR", "OFB", "CFB", "Random_Delta");
        modeSelect.setPlaceholder("Выберите режим");

        Select<String> paddingSelect = new Select<>();
        paddingSelect.setLabel("Паддинг");
        paddingSelect.setItems("PKCS7", "ZEROS", "ANSI_X923", "ISO_10126");
        paddingSelect.setPlaceholder("Выберите паддинг");

        Button confirm = new Button("Создать", ev -> {
            if (algorithmSelect.isEmpty() || modeSelect.isEmpty() || paddingSelect.isEmpty()) {
                Notification.show("Выберите все параметры шифрования!");
                return;
            }

            String algorithm = algorithmSelect.getValue();
            String mode = modeSelect.getValue();
            String padding = paddingSelect.getValue();

            try {
                String url = "http://localhost:8080/room/create"
                        + "?algorithm=" + algorithm
                        + "&mode=" + mode
                        + "&padding=" + padding;

                Map<?, ?> room = rest.postForObject(url, null, Map.class);
                if (room == null || room.get("id") == null) {
                    Notification.show("Ошибка: сервер не вернул ID комнаты");
                    return;
                }

                String dialogId = room.get("id").toString();
                Notification.show("Создана комната: " + dialogId);

                VaadinSession sess = VaadinSession.getCurrent();
                sess.setAttribute("algorithm", algorithm);
                sess.setAttribute("mode", mode);
                sess.setAttribute("padding", padding);

                dialog.close();
                getUI().ifPresent(ui -> ui.navigate("chat-view/" + dialogId));

            } catch (Exception ex) {
                Notification.show("Ошибка при создании комнаты: " + ex.getMessage());
            }
        });

        Button cancel = new Button("Отмена", e -> dialog.close());

        VerticalLayout content = new VerticalLayout(
                algorithmSelect,
                modeSelect,
                paddingSelect,
                confirm,
                cancel
        );
        dialog.add(content);
        dialog.open();
    }
}
