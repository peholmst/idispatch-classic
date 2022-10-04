package net.pkhapps.idispatch.server.ui.management;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import net.pkhapps.idispatch.server.Roles;
import net.pkhapps.idispatch.server.boundary.DestinationManagementService;
import net.pkhapps.idispatch.server.entity.Destination;
import net.pkhapps.idispatch.server.entity.Resource;

import javax.annotation.security.RolesAllowed;
import java.util.stream.Collectors;

@Route(value = "management/destination", layout = ManagementLayout.class)
@RolesAllowed(Roles.ROLE_ADMIN)
@PageTitle("iDispatch | Destinations")
public class DestinationManagementView extends AbstractManagementView<Destination, DestinationManagementService> {

    public DestinationManagementView(DestinationManagementService service) {
        super(service);
    }

    @Override
    protected void configureGrid(Grid<Destination> grid) {
        grid.addColumn(Destination::getType).setHeader("Type");
        grid.addColumn(destination -> destination.getResources().stream().map(Resource::getCallSign).sorted().collect(Collectors.joining(", "))).setHeader("Resources");
    }

    @Override
    protected AbstractManagementDialog<Destination> newDialog() {
        return new DestinationManagementDialog(getService());
    }
}
