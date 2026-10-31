package net.pkhapps.idispatch.server.municipality;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import net.pkhapps.idispatch.server.security.Roles;
import net.pkhapps.idispatch.server.shared.Bilingual;
import net.pkhapps.idispatch.server.shared.management.DefaultFilter;
import net.pkhapps.idispatch.server.ui.RootLayout;
import net.pkhapps.idispatch.server.ui.management.RecordForm;
import net.pkhapps.idispatch.server.ui.management.RecordManagementView;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

@Route(value = "management/municipality", layout = RootLayout.class)
@RolesAllowed(Roles.ROLE_ADMIN)
public class MunicipalityManagementView
        extends RecordManagementView<MunicipalityListItem, Municipality, MunicipalityDetails> {

    private final MunicipalityManagementService service;

    public MunicipalityManagementView(MunicipalityManagementService service) {
        // Set before the base class constructor, which lists the municipalities
        this.service = service;
        super("management.municipalities");
    }

    @Override
    protected void configureGrid(Grid<MunicipalityListItem> grid) {
        var nameFi = grid.addColumn(MunicipalityListItem::nameFi)
                .setHeader(getTranslation("management.municipality-name-fi")).setSortable(true);
        var nameSv = grid.addColumn(MunicipalityListItem::nameSv)
                .setHeader(getTranslation("management.municipality-name-sv")).setSortable(true);
        grid.sort(GridSortOrder.asc(Bilingual.isSwedish(getLocale()) ? nameSv : nameFi).build());
    }

    @Override
    protected List<MunicipalityListItem> findItems(DefaultFilter filter) {
        return service.findAll(filter);
    }

    @Override
    protected Optional<Municipality> findRecord(MunicipalityListItem item) {
        return service.findById(item.id());
    }

    @Override
    protected boolean isItemOf(MunicipalityListItem item, Municipality record) {
        return item.id().equals(record.id());
    }

    @Override
    protected boolean isItemActive(MunicipalityListItem item) {
        return item.active();
    }

    @Override
    protected boolean isRecordActive(Municipality record) {
        return record.active();
    }

    @Override
    protected RecordForm<MunicipalityDetails> createForm(@Nullable Municipality record) {
        return new MunicipalityForm(record);
    }

    @Override
    protected Municipality save(@Nullable Municipality record, MunicipalityDetails details) {
        return record == null ? service.create(details) : service.update(record.withDetails(details));
    }

    @Override
    protected void delete(Municipality record) {
        service.delete(record.id());
    }

    @Override
    protected void restore(Municipality record) {
        service.restore(record.id());
    }

    @Override
    protected String getTitle(@Nullable Municipality record) {
        return getTranslation(record == null ? "management.municipality.add" : "management.municipality.edit");
    }
}
