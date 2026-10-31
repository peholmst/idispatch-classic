package net.pkhapps.idispatch.server.ui.management;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import net.pkhapps.idispatch.server.boundary.DestinationManagementService;
import net.pkhapps.idispatch.server.entity.Destination;
import net.pkhapps.idispatch.server.entity.Resource;
import net.pkhapps.idispatch.server.entity.RunboardDestination;
import net.pkhapps.idispatch.server.entity.SmsDestination;
import net.pkhapps.idispatch.server.security.Roles;
import net.pkhapps.idispatch.server.ui.RootLayout;

import java.util.stream.Collectors;

@Route(value = "management/destination", layout = RootLayout.class)
@RolesAllowed(Roles.ROLE_ADMIN)
public class DestinationManagementView extends AbstractManagementView<Destination, DestinationManagementService> {

    public DestinationManagementView(DestinationManagementService service) {
        super(service, "management.destinations");
    }

    @Override
    protected void configureGrid(Grid<Destination> grid) {
        grid.addColumn(this::formatType).setHeader(getTranslation("management.type"));
        grid.addColumn(Destination::getDescription).setHeader(getTranslation("management.description"));
        grid.addColumn(destination -> DestinationForm.formatLanguage(destination.getLocale()))
                .setHeader(getTranslation("management.destination.language"));
        grid.addColumn(destination -> destination.getResources().stream().map(Resource::getCallSign).sorted().collect(Collectors.joining(", "))).setHeader(getTranslation("management.resources"));
    }

    private String formatType(Destination destination) {
        return getTranslation(destination instanceof SmsDestination
                ? "management.destination.type.sms" : "management.destination.type.runboard");
    }

    @Override
    protected Component createAddButton() {
        var menuBar = new MenuBar();
        menuBar.addThemeVariants(MenuBarVariant.PRIMARY);
        var add = menuBar.addItem(VaadinIcon.PLUS_CIRCLE.create());
        add.add(getTranslation("management.add"));
        var types = add.getSubMenu();
        types.addItem(getTranslation("management.destination.type.runboard"),
                event -> addNew(new RunboardDestination()));
        types.addItem(getTranslation("management.destination.type.sms"), event -> addNew(new SmsDestination()));
        return menuBar;
    }

    @Override
    protected ManagementForm<? extends Destination> createForm(Destination entity) {
        return switch (entity) {
            case SmsDestination sms -> {
                var form = new DestinationForm.Sms(getService());
                form.setFormDataObject(sms);
                yield form;
            }
            case RunboardDestination runboard -> {
                var form = new DestinationForm.Runboard(getService());
                form.setFormDataObject(runboard);
                yield form;
            }
            default -> throw new IllegalStateException("Unknown destination type");
        };
    }

    @Override
    protected String getTitle(Destination entity) {
        var sms = entity instanceof SmsDestination;
        if (entity.isNew()) {
            return getTranslation(sms ? "management.destination.add-sms" : "management.destination.add-runboard");
        } else {
            return getTranslation(sms ? "management.destination.edit-sms" : "management.destination.edit-runboard");
        }
    }
}
