package net.pkhapps.idispatch.server.assignmenttype;

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

@Route(value = "management/assignment-type", layout = RootLayout.class)
@RolesAllowed(Roles.ROLE_ADMIN)
public class AssignmentTypeManagementView
        extends RecordManagementView<AssignmentTypeListItem, AssignmentType, AssignmentTypeDetails> {

    private final AssignmentTypeManagementService service;

    public AssignmentTypeManagementView(AssignmentTypeManagementService service) {
        // Set before the base class constructor, which lists the types
        this.service = service;
        super("management.assignment-types");
    }

    @Override
    protected void configureGrid(Grid<AssignmentTypeListItem> grid) {
        var code = grid.addColumn(AssignmentTypeListItem::code).setHeader(getTranslation("management.code"))
                .setSortable(true);
        grid.addColumn(AssignmentTypeListItem::descriptionFi).setHeader(getTranslation("management.description-fi"));
        grid.addColumn(AssignmentTypeListItem::descriptionSv).setHeader(getTranslation("management.description-sv"));
        grid.sort(GridSortOrder.asc(code).build());
    }

    @Override
    protected List<AssignmentTypeListItem> findItems(DefaultFilter filter) {
        return service.findAll(filter);
    }

    @Override
    protected Optional<AssignmentType> findRecord(AssignmentTypeListItem item) {
        return service.findById(item.id());
    }

    @Override
    protected boolean isItemOf(AssignmentTypeListItem item, AssignmentType record) {
        return item.id().equals(record.id());
    }

    @Override
    protected boolean isItemActive(AssignmentTypeListItem item) {
        return item.active();
    }

    @Override
    protected boolean isRecordActive(AssignmentType record) {
        return record.active();
    }

    @Override
    protected RecordForm<AssignmentTypeDetails> createForm(@Nullable AssignmentType record) {
        return new AssignmentTypeForm(record);
    }

    @Override
    protected AssignmentType save(@Nullable AssignmentType record, AssignmentTypeDetails details) {
        return record == null ? service.create(details) : service.update(record.withDetails(details));
    }

    @Override
    protected void delete(AssignmentType record) {
        service.delete(record.id());
    }

    @Override
    protected void restore(AssignmentType record) {
        service.restore(record.id());
    }

    @Override
    protected String getTitle(@Nullable AssignmentType record) {
        return getTranslation(record == null ? "management.assignment-type.add" : "management.assignment-type.edit");
    }
}
