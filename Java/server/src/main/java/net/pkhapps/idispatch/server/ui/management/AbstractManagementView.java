package net.pkhapps.idispatch.server.ui.management;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.theme.lumo.LumoIcon;
import net.pkhapps.idispatch.server.boundary.DefaultFilter;
import net.pkhapps.idispatch.server.boundary.ManagementService;
import net.pkhapps.idispatch.server.entity.AbstractEntity;
import net.pkhapps.idispatch.server.entity.Deactivatable;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

@CssImport(value = "./styles/management-view-grid-styles.css", themeFor = "vaadin-grid")
public abstract class AbstractManagementView<E extends AbstractEntity, S extends ManagementService<E>>
        extends VerticalLayout {

    private final S service;
    private final Grid<E> grid;
    private final Button edit;
    private final Button delete;
    private final Button restore;
    private final Checkbox includeDeleted;
    private final TextField searchField;

    public AbstractManagementView(S service) {
        this.service = requireNonNull(service);

        grid = new Grid<>();
        configureGrid(grid);
        grid.setSizeFull();
        grid.setSelectionMode(Grid.SelectionMode.SINGLE);
        grid.setClassNameGenerator(entity -> isDeleted(entity) ? "deleted" : null);

        var add = new Button("Add...", VaadinIcon.PLUS_CIRCLE.create(), event -> add());
        add.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        edit = new Button(VaadinIcon.EDIT.create(), event -> edit());
        edit.setTooltipText("Edit");
        delete = new Button(VaadinIcon.CLOSE_CIRCLE.create(), event -> delete());
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
        restore = new Button(LumoIcon.UNDO.create(), event -> restore());

        searchField = new TextField();
        searchField.setPlaceholder("Search...");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(event -> refresh());

        includeDeleted = new Checkbox("Include deleted", event -> refresh());
        var refresh = new Button(VaadinIcon.REFRESH.create(), event -> refresh());

        setSizeFull();

        var filterBar = new HorizontalLayout();
        filterBar.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        filterBar.add(searchField);
        if (supportsRestore()) {
            filterBar.add(includeDeleted);
        }
        filterBar.add(refresh);
        add(filterBar);

        add(grid);

        var buttons = new HorizontalLayout();
        buttons.add(add, edit);
        if (supportsDelete()) {
            buttons.add(delete);
        }
        if (supportsRestore()) {
            buttons.add(restore);
        }

        add(buttons);

        grid.addSelectionListener(event -> updateButtonStates());

        updateButtonStates();
        refresh();
    }

    protected boolean supportsDelete() {
        return (getService() instanceof ManagementService.HardDeletable) || (getService() instanceof ManagementService.SoftDeletable);
    }

    protected boolean supportsRestore() {
        return getService() instanceof ManagementService.SoftDeletable;
    }

    protected Optional<E> getSelection() {
        return grid.getSelectionModel().getFirstSelectedItem();
    }

    private void refresh() {
        var filter = new DefaultFilter(includeDeleted.getValue(), searchField.getValue());
        grid.setItems(getService().findAll(filter));
    }

    private void add() {
        newDialog().openNew(this::refresh);
    }

    private void edit() {
        getSelection().ifPresent(selected -> newDialog().open(selected, this::refresh));
    }

    private void delete() {
        getSelection().ifPresent(selected -> {
            delete(selected);
            refresh();
        });
    }

    private void restore() {
        getSelection().ifPresent(selected -> {
            restore(selected);
            refresh();
        });
    }

    private void updateButtonStates() {
        edit.setEnabled(getSelection().map(this::canEdit).orElse(false));
        delete.setEnabled(getSelection().map(this::canDelete).orElse(false));
        restore.setEnabled(getSelection().map(this::canRestore).orElse(false));
    }

    protected boolean canEdit(E entity) {
        if (entity instanceof Deactivatable) {
            return ((Deactivatable) entity).isActive();
        } else {
            return !entity.isNew();
        }
    }

    protected boolean canDelete(E entity) {
        if (entity instanceof Deactivatable) {
            return ((Deactivatable) entity).isActive();
        } else {
            return !entity.isNew();
        }
    }

    protected boolean canRestore(E entity) {
        return entity instanceof Deactivatable && !((Deactivatable) entity).isActive();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected void delete(E entity) {
        if (getService() instanceof ManagementService.HardDeletable<?>) {
            ((ManagementService.HardDeletable) getService()).delete(entity);
        } else if (getService() instanceof ManagementService.SoftDeletable<?>) {
            ((ManagementService.SoftDeletable) getService()).delete(entity);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected void restore(E entity) {
        if (getService() instanceof ManagementService.SoftDeletable<?>) {
            ((ManagementService.SoftDeletable) getService()).restore(entity);
        }
    }

    protected boolean isDeleted(E entity) {
        if (entity instanceof Deactivatable) {
            return !((Deactivatable) entity).isActive();
        } else {
            return false;
        }
    }

    protected abstract void configureGrid(Grid<E> grid);

    protected abstract AbstractManagementDialog<E> newDialog();

    protected S getService() {
        return service;
    }
}
