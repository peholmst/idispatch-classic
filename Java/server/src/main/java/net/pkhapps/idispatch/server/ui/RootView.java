package net.pkhapps.idispatch.server.ui;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

@Route(value = "", layout = RootLayout.class)
@PermitAll
public class RootView extends VerticalLayout implements HasDynamicTitle {

    public RootView() {
        add(new ViewTitle(getTranslation("nav.home")), new H1(getTranslation("home.welcome")));
    }

    @Override
    public String getPageTitle() {
        return "iDispatch";
    }
}
