package com.project.vaadin;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

@PageTitle("Dialog")
@Route(value = "dialog", layout = MainLayout.class)
public class DialogView extends VerticalLayout {

    public DialogView() {
        TextField dialogIdField = new TextField("ID комнаты для подключения");

        Button createButton = new Button("Создать новую комнату", event -> {
            try {
                RestTemplate rest = new RestTemplate();
                String url = "http://localhost:8080/room/create?algorithm=MARS&mode=CBC&padding=PKCS7";
                Map<?, ?> room = rest.postForObject(url, null, Map.class);
                String dialogId = (String) room.get("id");
                Notification.show("Создана комната: " + dialogId);
                getUI().ifPresent(ui -> ui.navigate("chat-view/" + dialogId));
            } catch (Exception e) {
                Notification.show("Ошибка создания комнаты: " + e.getMessage());
            }
        });

        Button joinButton = new Button("Подключиться", event -> {
            String dialogId = dialogIdField.getValue().trim();
            if (!dialogId.isEmpty()) {
                getUI().ifPresent(ui -> ui.navigate("chat-view/" + dialogId));
            }
        });

        add(dialogIdField, joinButton, createButton);
    }

    public void beforeEnter(BeforeEnterEvent event) {
        Object saved = VaadinSession.getCurrent().getAttribute("username");
        if (saved == null) {
            event.forwardTo(LoginView.class);
        }
    }

}
