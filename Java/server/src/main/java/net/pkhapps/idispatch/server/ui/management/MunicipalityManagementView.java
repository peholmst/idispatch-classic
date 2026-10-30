package net.pkhapps.idispatch.server.ui.management;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import net.pkhapps.idispatch.server.boundary.MunicipalityManagementService;
import net.pkhapps.idispatch.server.entity.Municipality;
import net.pkhapps.idispatch.server.security.Roles;
import net.pkhapps.idispatch.server.ui.RootLayout;


@Route(value = "management/municipality", layout = RootLayout.class)
@RolesAllowed(Roles.ROLE_ADMIN)
public class MunicipalityManagementView extends AbstractManagementView<Municipality, MunicipalityManagementService> {

    public MunicipalityManagementView(MunicipalityManagementService service) {
        super(service, "management.municipalities");
    }

    @Override
    protected void configureGrid(Grid<Municipality> grid) {
        var name = grid.addColumn(Municipality::getName).setHeader(getTranslation("management.municipality"));
        grid.sort(GridSortOrder.asc(name).build());
    }

    @Override
    protected Component createAddButton() {
        return addButton(Municipality::new);
    }

    @Override
    protected ManagementForm<Municipality> createForm(Municipality entity) {
        var form = new MunicipalityForm();
        form.setFormDataObject(entity);
        return form;
    }

    @Override
    protected String getTitle(Municipality entity) {
        return getTranslation(entity.isNew() ? "management.municipality.add" : "management.municipality.edit");
    }
}
