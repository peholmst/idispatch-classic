package net.pkhapps.idispatch.server.ui.management;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import net.pkhapps.idispatch.server.boundary.UserManagementService;
import net.pkhapps.idispatch.server.entity.User;
import net.pkhapps.idispatch.server.security.Roles;
import net.pkhapps.idispatch.server.ui.Formatters;

@Route(value = "management/user", layout = ManagementLayout.class)
@RolesAllowed(Roles.ROLE_ADMIN)
@PageTitle("iDispatch | User")
public class UserManagementView extends AbstractManagementView<User, UserManagementService> {

    public UserManagementView(UserManagementService service) {
        super(service);
    }

    @Override
    protected void configureGrid(Grid<User> grid) {
        var username = grid.addColumn(User::getUsername).setHeader("Username").setSortable(true);
        grid.addColumn(user -> Formatters.formatBoolean(user.isAdmin())).setHeader("Administrator");
        grid.addColumn(user -> Formatters.formatBoolean(user.isDispatcher())).setHeader("Dispatcher");
        grid.addColumn(user -> Formatters.formatBoolean(user.isReportReader())).setHeader("Report Reader");
        grid.addColumn(user -> Formatters.formatBoolean(user.isEnabled())).setHeader("Enabled");
        grid.sort(GridSortOrder.asc(username).build());
    }

    // TODO Reset password

    @Override
    protected AbstractManagementDialog<User> newDialog() {
        return new UserManagementDialog(getService());
    }
}
