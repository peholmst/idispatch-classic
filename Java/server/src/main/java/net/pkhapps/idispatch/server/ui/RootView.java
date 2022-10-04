package net.pkhapps.idispatch.server.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import net.pkhapps.idispatch.server.ui.dws.DispatchView;
import net.pkhapps.idispatch.server.ui.management.ManagementView;

import javax.annotation.security.PermitAll;

@Route("")
@PageTitle("iDispatch")
@PermitAll
public class RootView extends VerticalLayout {

    public RootView() {
        var management = new Button("Management UI", event -> getUI().ifPresent(ui -> ui.navigate(ManagementView.class)));
        var dws = new Button("Dispatch UI", event -> getUI().ifPresent(ui -> ui.navigate(DispatchView.class)));
        add(new H1("Welcome to iDispatch!"), management, dws);
    }
}
