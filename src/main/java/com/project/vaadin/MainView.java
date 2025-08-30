package com.project.vaadin;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

@PageTitle("Main")
@Route(value = "")
public class MainView extends VerticalLayout {

    public MainView() {
        add(new RouterLink("Перейти к логину", LoginView.class));
    }
}
