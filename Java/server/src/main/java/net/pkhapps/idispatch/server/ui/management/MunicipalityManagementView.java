package net.pkhapps.idispatch.server.ui.management;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.router.Route;
import net.pkhapps.idispatch.server.boundary.MunicipalityManagementService;
import net.pkhapps.idispatch.server.entity.Municipality;


@Route(value = "management/municipality", layout = ManagementLayout.class)
public class MunicipalityManagementView extends AbstractManagementView<Municipality, MunicipalityManagementService> {

    public MunicipalityManagementView(MunicipalityManagementService service) {
        super(service, Municipality.class);
    }

    @Override
    protected void configureGrid(Grid<Municipality> grid) {
        grid.addColumn(Municipality::getName).setHeader("Municipality");
    }

    @Override
    protected AbstractManagementDialog<Municipality> newDialog() {
        return new MunicipalityManagementDialog(getService());
    }
}
