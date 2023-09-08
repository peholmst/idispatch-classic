package net.pkhapps.idispatch.server.ui.management;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import net.pkhapps.idispatch.server.boundary.UserManagementService;
import net.pkhapps.idispatch.server.entity.User;
import net.pkhapps.idispatch.server.security.Roles;

@Route(value = "management/user", layout = ManagementLayout.class)
@RolesAllowed(Roles.ROLE_ADMIN)
@PageTitle("iDispatch | User")
public class UserManagementView extends AbstractManagementView<User, UserManagementService> {

    // TODO Implement me!

    public UserManagementView(UserManagementService service) {
        super(service);
    }

    @Override
    protected void configureGrid(Grid<User> grid) {

    }

    @Override
    protected AbstractManagementDialog<User> newDialog() {
        return null;
    }
}
