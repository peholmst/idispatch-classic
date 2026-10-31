package net.pkhapps.idispatch.server.resource;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import net.pkhapps.idispatch.server.security.Roles;
import net.pkhapps.idispatch.server.ui.RootLayout;
import net.pkhapps.idispatch.server.ui.management.AbstractManagementView;
import net.pkhapps.idispatch.server.ui.management.CodeAndDescriptionForm;
import net.pkhapps.idispatch.server.ui.management.ManagementForm;

@Route(value = "management/resource-type", layout = RootLayout.class)
@RolesAllowed(Roles.ROLE_ADMIN)
public class ResourceTypeManagementView extends AbstractManagementView<ResourceType, ResourceTypeManagementService> {

    public ResourceTypeManagementView(ResourceTypeManagementService service) {
        super(service, "management.resource-types");
    }

    @Override
    protected void configureGrid(Grid<ResourceType> grid) {
        var code = grid.addColumn(ResourceType::getCode).setHeader(getTranslation("management.code")).setSortable(true);
        grid.addColumn(ResourceType::getDescriptionFi).setHeader(getTranslation("management.description-fi"));
        grid.addColumn(ResourceType::getDescriptionSv).setHeader(getTranslation("management.description-sv"));
        grid.sort(GridSortOrder.asc(code).build());
    }

    @Override
    protected Component createAddButton() {
        return addButton(ResourceType::new);
    }

    @Override
    protected ManagementForm<ResourceType> createForm(ResourceType entity) {
        var form = new CodeAndDescriptionForm<>(ResourceType.class);
        form.setFormDataObject(entity);
        return form;
    }

    @Override
    protected String getTitle(ResourceType entity) {
        return getTranslation(entity.isNew() ? "management.resource-type.add" : "management.resource-type.edit");
    }
}
