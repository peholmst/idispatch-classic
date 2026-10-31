package net.pkhapps.idispatch.server.dispatch;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import net.pkhapps.idispatch.server.dispatch.runboard.RunboardDestination;
import net.pkhapps.idispatch.server.dispatch.sms.SmsDestination;
import net.pkhapps.idispatch.server.resource.ResourceId;
import net.pkhapps.idispatch.server.resource.ResourceListItem;
import net.pkhapps.idispatch.server.security.Roles;
import net.pkhapps.idispatch.server.ui.RootLayout;
import net.pkhapps.idispatch.server.ui.management.AbstractManagementView;
import net.pkhapps.idispatch.server.ui.management.ManagementForm;

import java.util.Set;
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
        grid.addColumn(destination -> callSigns(destination.getResources())).setHeader(getTranslation("management.resources"));
    }

    private String callSigns(Set<ResourceId> resources) {
        return getService().findResources(resources).stream().map(ResourceListItem::callSign)
                .collect(Collectors.joining(", "));
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
