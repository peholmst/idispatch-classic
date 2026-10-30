package net.pkhapps.idispatch.server.ui.management;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import net.pkhapps.idispatch.server.boundary.ResourceManagementService;
import net.pkhapps.idispatch.server.entity.Resource;
import net.pkhapps.idispatch.server.security.Roles;
import net.pkhapps.idispatch.server.ui.RootLayout;

@Route(value = "management/resource", layout = RootLayout.class)
@RolesAllowed(Roles.ROLE_ADMIN)
public class ResourceManagementView extends AbstractManagementView<Resource, ResourceManagementService> {

    public ResourceManagementView(ResourceManagementService service) {
        super(service, "management.resources");
    }

    @Override
    protected void configureGrid(Grid<Resource> grid) {
        var callSign = grid.addColumn(Resource::getCallSign).setHeader(getTranslation("management.call-sign")).setSortable(true);
        grid.addColumn(r -> r.getType().getDescription()).setHeader(getTranslation("management.type"));
        grid.sort(GridSortOrder.asc(callSign).build());
    }

    @Override
    protected AbstractManagementDialog<Resource> newDialog() {
        return new ResourceManagementDialog(getService());
    }
}
