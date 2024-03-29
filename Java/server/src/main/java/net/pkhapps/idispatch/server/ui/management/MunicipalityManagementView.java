package net.pkhapps.idispatch.server.ui.management;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import net.pkhapps.idispatch.server.boundary.MunicipalityManagementService;
import net.pkhapps.idispatch.server.entity.Municipality;
import net.pkhapps.idispatch.server.security.Roles;


@Route(value = "management/municipality", layout = ManagementLayout.class)
@RolesAllowed(Roles.ROLE_ADMIN)
@PageTitle("iDispatch | Municipalities")
public class MunicipalityManagementView extends AbstractManagementView<Municipality, MunicipalityManagementService> {

    public MunicipalityManagementView(MunicipalityManagementService service) {
        super(service);
    }

    @Override
    protected void configureGrid(Grid<Municipality> grid) {
        var name = grid.addColumn(Municipality::getName).setHeader("Municipality");
        grid.sort(GridSortOrder.asc(name).build());
    }

    @Override
    protected AbstractManagementDialog<Municipality> newDialog() {
        return new MunicipalityManagementDialog(getService());
    }
}
