package net.pkhapps.idispatch.server.ui;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

@Route(value = "", layout = RootLayout.class)
@PermitAll
public class RootView extends VerticalLayout implements HasNavbarContent, HasDynamicTitle {

    public RootView() {
        add(new H1(getTranslation("home.welcome")));
    }

    @Override
    public String getTitle() {
        return getTranslation("nav.home");
    }

    @Override
    public String getPageTitle() {
        return "iDispatch";
    }
}
