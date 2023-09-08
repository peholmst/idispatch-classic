package net.pkhapps.idispatch.server.ui.reports;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import net.pkhapps.idispatch.server.security.Roles;
import net.pkhapps.idispatch.server.ui.HasNavbarContent;
import net.pkhapps.idispatch.server.ui.RootLayout;

@Route(value = "reports", layout = RootLayout.class)
@RolesAllowed(Roles.ROLE_REPORT_READER)
@PageTitle("iDispatch | Reports")
@JsModule("@vaadin/vaadin-lumo-styles/presets/compact.js")
public class ReportsView extends VerticalLayout implements HasNavbarContent {

    // TODO Implement me!

    @Override
    public Component getNavbarContent() {
        return new Div();
    }

    @Override
    public String getTitle() {
        return "Reports";
    }
}
