package net.pkhapps.idispatch.server.ui.management;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import net.pkhapps.idispatch.server.boundary.ResourceTypeManagementService;
import net.pkhapps.idispatch.server.entity.ResourceType;

@Route(value = "management/resource-type", layout = ManagementLayout.class)
@PageTitle("iDispatch | Resource Types")
public class ResourceTypeManagementView extends AbstractManagementView<ResourceType, ResourceTypeManagementService> {

    public ResourceTypeManagementView(ResourceTypeManagementService service) {
        super(service, ResourceType.class);
    }

    @Override
    protected void configureGrid(Grid<ResourceType> grid) {
        var code = grid.addColumn(ResourceType::getCode).setHeader("Code").setSortable(true);
        grid.addColumn(ResourceType::getDescription).setHeader("Description");
        grid.sort(GridSortOrder.asc(code).build());
    }

    @Override
    protected AbstractManagementDialog<ResourceType> newDialog() {
        return new ResourceTypeManagementDialog(getService());
    }
}