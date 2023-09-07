package net.pkhapps.idispatch.server.ui.management;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import net.pkhapps.idispatch.server.Roles;

@Route(value = "management", layout = ManagementLayout.class)
@RolesAllowed(Roles.ROLE_ADMIN)
@PageTitle("iDispatch | Management")
public class ManagementView extends VerticalLayout {

    public ManagementView() {
        add(new Span("Please select a menu item on the left"));
    }
}
