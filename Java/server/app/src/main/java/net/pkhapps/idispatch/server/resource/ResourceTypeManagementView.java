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

@Route(value = "management/resource-type", layout = RootLayout.class)
@RolesAllowed(Roles.ROLE_ADMIN)
public class ResourceTypeManagementView
        extends RecordManagementView<ResourceTypeListItem, ResourceType, ResourceTypeDetails> {

    private final ResourceTypeManagementService service;

    public ResourceTypeManagementView(ResourceTypeManagementService service) {
        // Set before the base class constructor, which lists the types
        this.service = service;
        super("management.resource-types");
    }

    @Override
    protected void configureGrid(Grid<ResourceTypeListItem> grid) {
        var code = grid.addColumn(ResourceTypeListItem::code).setHeader(getTranslation("management.code"))
                .setSortable(true);
        grid.addColumn(ResourceTypeListItem::descriptionFi).setHeader(getTranslation("management.description-fi"));
        grid.addColumn(ResourceTypeListItem::descriptionSv).setHeader(getTranslation("management.description-sv"));
        grid.sort(GridSortOrder.asc(code).build());
    }

    @Override
    protected List<ResourceTypeListItem> findItems(DefaultFilter filter) {
        return service.findAll(filter);
    }

    @Override
    protected Optional<ResourceType> findRecord(ResourceTypeListItem item) {
        return service.findById(item.id());
    }

    @Override
    protected boolean isItemOf(ResourceTypeListItem item, ResourceType record) {
        return item.id().equals(record.id());
    }

    @Override
    protected boolean isItemActive(ResourceTypeListItem item) {
        return item.active();
    }

    @Override
    protected boolean isRecordActive(ResourceType record) {
        return record.active();
    }

    @Override
    protected RecordForm<ResourceTypeDetails> createForm(@Nullable ResourceType record) {
        return new ResourceTypeForm(record);
    }

    @Override
    protected ResourceType save(@Nullable ResourceType record, ResourceTypeDetails details) {
        return record == null ? service.create(details) : service.update(record.withDetails(details));
    }

    @Override
    protected void delete(ResourceType record) {
        service.delete(record.id());
    }

    @Override
    protected void restore(ResourceType record) {
        service.restore(record.id());
    }

    @Override
    protected String getTitle(@Nullable ResourceType record) {
        return getTranslation(record == null ? "management.resource-type.add" : "management.resource-type.edit");
    }
}
