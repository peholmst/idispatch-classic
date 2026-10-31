package net.pkhapps.idispatch.server.resource;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
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

@Route(value = "management/resource", layout = RootLayout.class)
@RolesAllowed(Roles.ROLE_ADMIN)
public class ResourceManagementView extends RecordManagementView<ResourceListItem, Resource, ResourceDetails> {

    private final ResourceManagementService service;

    public ResourceManagementView(ResourceManagementService service) {
        // Set before the base class constructor, which lists the resources
        this.service = service;
        super("management.resources");
    }

    @Override
    protected void configureGrid(Grid<ResourceListItem> grid) {
        var callSign = grid.addColumn(ResourceListItem::callSign).setHeader(getTranslation("management.call-sign"))
                .setSortable(true);
        grid.addColumn(item -> item.typeDescription(getLocale())).setHeader(getTranslation("management.type"));
        grid.sort(GridSortOrder.asc(callSign).build());
    }

    @Override
    protected List<ResourceListItem> findItems(DefaultFilter filter) {
        return service.findAll(filter);
    }

    @Override
    protected Optional<Resource> findRecord(ResourceListItem item) {
        return service.findById(item.id());
    }

    @Override
    protected boolean isItemOf(ResourceListItem item, Resource record) {
        return item.id().equals(record.id());
    }

    @Override
    protected boolean isItemActive(ResourceListItem item) {
        return item.active();
    }

    @Override
    protected boolean isRecordActive(Resource record) {
        return record.active();
    }

    @Override
    protected RecordForm<ResourceDetails> createForm(@Nullable Resource record) {
        return new ResourceForm(service, record);
    }

    @Override
    protected Resource save(@Nullable Resource record, ResourceDetails details) {
        return record == null ? service.create(details) : service.update(record.withDetails(details));
    }

    @Override
    protected void delete(Resource record) {
        service.delete(record.id());
    }

    @Override
    protected void restore(Resource record) {
        service.restore(record.id());
    }

    @Override
    protected String getTitle(@Nullable Resource record) {
        return getTranslation(record == null ? "management.resource.add" : "management.resource.edit");
    }
}
