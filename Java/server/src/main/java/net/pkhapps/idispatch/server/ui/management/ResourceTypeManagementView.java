package net.pkhapps.idispatch.server.ui.management;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import net.pkhapps.idispatch.server.boundary.ResourceTypeManagementService;
import net.pkhapps.idispatch.server.entity.ResourceType;
import net.pkhapps.idispatch.server.security.Roles;
import net.pkhapps.idispatch.server.ui.RootLayout;

@Route(value = "management/resource-type", layout = RootLayout.class)
@RolesAllowed(Roles.ROLE_ADMIN)
public class ResourceTypeManagementView extends AbstractManagementView<ResourceType, ResourceTypeManagementService> {

    public ResourceTypeManagementView(ResourceTypeManagementService service) {
        super(service, "management.resource-types");
    }

    @Override
    protected void configureGrid(Grid<ResourceType> grid) {
        var code = grid.addColumn(ResourceType::getCode).setHeader(getTranslation("management.code")).setSortable(true);
        grid.addColumn(ResourceType::getDescription).setHeader(getTranslation("management.description"));
        grid.sort(GridSortOrder.asc(code).build());
    }

    @Override
    protected AbstractManagementDialog<ResourceType> newDialog() {
        return new ResourceTypeManagementDialog(getService());
    }
}
