package net.pkhapps.idispatch.server.ui.management;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import net.pkhapps.idispatch.server.Roles;
import net.pkhapps.idispatch.server.boundary.AssignmentTypeManagementService;
import net.pkhapps.idispatch.server.entity.AssignmentType;

import javax.annotation.security.RolesAllowed;

@Route(value = "management/assignment-type", layout = ManagementLayout.class)
@RolesAllowed(Roles.ROLE_ADMIN)
@PageTitle("iDispatch | Assignment Types")
public class AssignmentTypeManagementView extends AbstractManagementView<AssignmentType, AssignmentTypeManagementService> {

    public AssignmentTypeManagementView(AssignmentTypeManagementService service) {
        super(service);
    }

    @Override
    protected void configureGrid(Grid<AssignmentType> grid) {
        var code = grid.addColumn(AssignmentType::getCode).setHeader("Code").setSortable(true);
        grid.addColumn(AssignmentType::getDescription).setHeader("Description");
        grid.sort(GridSortOrder.asc(code).build());
    }

    @Override
    protected AbstractManagementDialog<AssignmentType> newDialog() {
        return new AssignmentTypeManagementDialog(getService());
    }
}
