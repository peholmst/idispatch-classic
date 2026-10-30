package net.pkhapps.idispatch.server.ui.management;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import net.pkhapps.idispatch.server.boundary.DestinationManagementService;
import net.pkhapps.idispatch.server.entity.Destination;
import net.pkhapps.idispatch.server.entity.Resource;
import net.pkhapps.idispatch.server.entity.SmsDestination;
import net.pkhapps.idispatch.server.security.Roles;

import java.util.stream.Collectors;

@Route(value = "management/destination", layout = ManagementLayout.class)
@RolesAllowed(Roles.ROLE_ADMIN)
public class DestinationManagementView extends AbstractManagementView<Destination, DestinationManagementService> {

    public DestinationManagementView(DestinationManagementService service) {
        super(service);
    }

    @Override
    protected void configureGrid(Grid<Destination> grid) {
        grid.addColumn(this::formatType).setHeader(getTranslation("management.type"));
        grid.addColumn(Destination::getDescription).setHeader(getTranslation("management.description"));
        grid.addColumn(destination -> destination.getResources().stream().map(Resource::getCallSign).sorted().collect(Collectors.joining(", "))).setHeader(getTranslation("management.resources"));
    }

    private String formatType(Destination destination) {
        return getTranslation(destination instanceof SmsDestination
                ? "management.destination.type.sms" : "management.destination.type.runboard");
    }

    @Override
    protected AbstractManagementDialog<Destination> newDialog() {
        return new DestinationManagementDialog(getService());
    }
}
