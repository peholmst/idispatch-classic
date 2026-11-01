package net.pkhapps.idispatch.server.dispatch;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import net.pkhapps.idispatch.server.security.Roles;
import net.pkhapps.idispatch.server.shared.management.DefaultFilter;
import net.pkhapps.idispatch.server.ui.RootLayout;
import net.pkhapps.idispatch.server.ui.management.RecordForm;
import net.pkhapps.idispatch.server.ui.management.RecordManagementView;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

@Route(value = "management/destination", layout = RootLayout.class)
@RolesAllowed(Roles.ROLE_ADMIN)
public class DestinationManagementView
        extends RecordManagementView<DestinationListItem, Destination, DestinationDetails> {

    private final DestinationManagementService service;
    // The mobile destination just created and its key, which its form shows once
    private DestinationManagementService.@Nullable KeyedMobileDestination justCreated;

    public DestinationManagementView(DestinationManagementService service) {
        // Set before the base class constructor, which lists the destinations
        this.service = service;
        super("management.destinations");
    }

    @Override
    protected void configureGrid(Grid<DestinationListItem> grid) {
        grid.addColumn(item -> formatKind(item.kind())).setHeader(getTranslation("management.type"));
        grid.addColumn(DestinationListItem::description).setHeader(getTranslation("management.description"));
        grid.addColumn(item -> DestinationForm.formatLanguage(item.locale()))
                .setHeader(getTranslation("management.destination.language"));
        grid.addColumn(item -> String.join(", ", item.callSigns())).setHeader(getTranslation("management.resources"));
    }

    private String formatKind(DestinationListItem.Kind kind) {
        return getTranslation(switch (kind) {
            case SMS -> "management.destination.type.sms";
            case RUNBOARD -> "management.destination.type.runboard";
            case MOBILE -> "management.destination.type.mobile";
        });
    }

    @Override
    protected Component createAddButton() {
        var menuBar = new MenuBar();
        menuBar.addThemeVariants(MenuBarVariant.PRIMARY);
        var add = menuBar.addItem(VaadinIcon.PLUS_CIRCLE.create());
        add.add(getTranslation("management.add"));
        var kinds = add.getSubMenu();
        kinds.addItem(formatKind(DestinationListItem.Kind.RUNBOARD),
                event -> startAdding(DestinationListItem.Kind.RUNBOARD));
        kinds.addItem(formatKind(DestinationListItem.Kind.SMS), event -> startAdding(DestinationListItem.Kind.SMS));
        kinds.addItem(formatKind(DestinationListItem.Kind.MOBILE),
                event -> startAdding(DestinationListItem.Kind.MOBILE));
        return menuBar;
    }

    @Override
    protected List<DestinationListItem> findItems(DefaultFilter filter) {
        return service.findAll(filter);
    }

    @Override
    protected Optional<Destination> findRecord(DestinationListItem item) {
        return service.findById(item.id());
    }

    @Override
    protected boolean isItemOf(DestinationListItem item, Destination record) {
        return item.id().equals(record.id());
    }

    @Override
    protected boolean isItemActive(DestinationListItem item) {
        return item.active();
    }

    @Override
    protected boolean isRecordActive(Destination record) {
        return record.active();
    }

    @Override
    protected RecordForm<DestinationDetails> createForm(@Nullable Destination record) {
        return switch (record) {
            case SmsDestination sms -> new DestinationForm.Sms(service, sms);
            case RunboardDestination runboard -> new DestinationForm.Runboard(service, runboard);
            case MobileDestination mobile -> new DestinationForm.Mobile(service, mobile, takeNewKey(mobile));
            case null -> throw new IllegalArgumentException("A new destination is added by its kind");
        };
    }

    /**
     * Returns the key of the mobile destination if it was just created, only once.
     */
    private @Nullable MobileKey takeNewKey(MobileDestination mobile) {
        var created = justCreated;
        justCreated = null;
        return created != null && created.destination().id().equals(mobile.id()) ? created.key() : null;
    }

    @Override
    protected RecordForm<DestinationDetails> createFormToAdd(@Nullable Object kind) {
        return switch ((DestinationListItem.Kind) requireNonNull(kind)) {
            case SMS -> new DestinationForm.Sms(service, null);
            case RUNBOARD -> new DestinationForm.Runboard(service, null);
            case MOBILE -> new DestinationForm.Mobile(service, null, null);
        };
    }

    @Override
    protected Destination save(@Nullable Destination record, DestinationDetails details) {
        if (record == null && details instanceof MobileDestinationDetails mobileDetails) {
            justCreated = service.createMobile(mobileDetails);
            return justCreated.destination();
        }
        return switch (record) {
            case null -> service.create(details);
            case SmsDestination sms when details instanceof SmsDestinationDetails smsDetails ->
                    service.update(sms.withDetails(smsDetails));
            case RunboardDestination runboard when details instanceof RunboardDestinationDetails runboardDetails ->
                    service.update(runboard.withDetails(runboardDetails));
            case MobileDestination mobile when details instanceof MobileDestinationDetails mobileDetails ->
                    service.update(mobile.withDetails(mobileDetails));
            default -> throw new IllegalArgumentException("The details are of another kind of destination");
        };
    }

    @Override
    protected void delete(Destination record) {
        service.delete(record.id());
    }

    @Override
    protected void restore(Destination record) {
        service.restore(record.id());
    }

    @Override
    protected String getTitle(@Nullable Destination record) {
        return getTranslation(switch (record) {
            case SmsDestination sms -> "management.destination.edit-sms";
            case RunboardDestination runboard -> "management.destination.edit-runboard";
            case MobileDestination mobile -> "management.destination.edit-mobile";
            case null -> throw new IllegalArgumentException("A new destination is added by its kind");
        });
    }

    @Override
    protected String getTitleToAdd(@Nullable Object kind) {
        return getTranslation(switch ((DestinationListItem.Kind) requireNonNull(kind)) {
            case SMS -> "management.destination.add-sms";
            case RUNBOARD -> "management.destination.add-runboard";
            case MOBILE -> "management.destination.add-mobile";
        });
    }
}
