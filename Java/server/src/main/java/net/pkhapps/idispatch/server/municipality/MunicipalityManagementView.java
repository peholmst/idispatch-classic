package net.pkhapps.idispatch.server.municipality;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import net.pkhapps.idispatch.server.security.Roles;
import net.pkhapps.idispatch.server.shared.Bilingual;
import net.pkhapps.idispatch.server.ui.RootLayout;
import net.pkhapps.idispatch.server.ui.management.AbstractManagementView;
import net.pkhapps.idispatch.server.ui.management.ManagementForm;


@Route(value = "management/municipality", layout = RootLayout.class)
@RolesAllowed(Roles.ROLE_ADMIN)
public class MunicipalityManagementView extends AbstractManagementView<Municipality, MunicipalityManagementService> {

    public MunicipalityManagementView(MunicipalityManagementService service) {
        super(service, "management.municipalities");
    }

    @Override
    protected void configureGrid(Grid<Municipality> grid) {
        var nameFi = grid.addColumn(Municipality::getNameFi).setHeader(getTranslation("management.municipality-name-fi"))
                .setSortable(true);
        var nameSv = grid.addColumn(Municipality::getNameSv).setHeader(getTranslation("management.municipality-name-sv"))
                .setSortable(true);
        grid.sort(GridSortOrder.asc(Bilingual.isSwedish(getLocale()) ? nameSv : nameFi).build());
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
