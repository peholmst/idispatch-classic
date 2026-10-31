package net.pkhapps.idispatch.server.ui.management;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import net.pkhapps.idispatch.server.boundary.AssignmentTypeManagementService;
import net.pkhapps.idispatch.server.entity.AssignmentType;
import net.pkhapps.idispatch.server.security.Roles;
import net.pkhapps.idispatch.server.ui.RootLayout;

@Route(value = "management/assignment-type", layout = RootLayout.class)
@RolesAllowed(Roles.ROLE_ADMIN)
public class AssignmentTypeManagementView extends AbstractManagementView<AssignmentType, AssignmentTypeManagementService> {

    public AssignmentTypeManagementView(AssignmentTypeManagementService service) {
        super(service, "management.assignment-types");
    }

    @Override
    protected void configureGrid(Grid<AssignmentType> grid) {
        var code = grid.addColumn(AssignmentType::getCode).setHeader(getTranslation("management.code")).setSortable(true);
        grid.addColumn(AssignmentType::getDescriptionFi).setHeader(getTranslation("management.description-fi"));
        grid.addColumn(AssignmentType::getDescriptionSv).setHeader(getTranslation("management.description-sv"));
        grid.sort(GridSortOrder.asc(code).build());
    }

    @Override
    protected Component createAddButton() {
        return addButton(AssignmentType::new);
    }

    @Override
    protected ManagementForm<AssignmentType> createForm(AssignmentType entity) {
        var form = new CodeAndDescriptionForm<>(AssignmentType.class);
        form.setFormDataObject(entity);
        return form;
    }

    @Override
    protected String getTitle(AssignmentType entity) {
        return getTranslation(entity.isNew() ? "management.assignment-type.add" : "management.assignment-type.edit");
    }
}
