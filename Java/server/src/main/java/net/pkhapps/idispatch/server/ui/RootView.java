package net.pkhapps.idispatch.server.ui;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

@Route(value = "", layout = RootLayout.class)
@PageTitle("iDispatch")
@PermitAll
public class RootView extends VerticalLayout {

    public RootView() {
        add(new H1("Welcome to iDispatch!"));
    }
}
