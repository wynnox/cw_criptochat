package com.project.vaadin;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

@PageTitle("Login")
@Route(value = "login")
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    public LoginView() {
        TextField usernameField = new TextField("Введите имя пользователя");
        Button loginButton = new Button("Войти", event -> {
            String username = usernameField.getValue().trim();
            if (!username.isEmpty()) {
                VaadinSession.getCurrent().setAttribute("username", username);
                getUI().ifPresent(ui -> ui.navigate("dialog"));
            } else {
                Notification.show("Введите имя");
            }
        });

        add(usernameField, loginButton);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Object saved = VaadinSession.getCurrent().getAttribute("username");
        if (saved != null) {
            event.forwardTo("dialog");
        }
    }
}
