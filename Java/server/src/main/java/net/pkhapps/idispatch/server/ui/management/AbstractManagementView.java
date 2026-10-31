package net.pkhapps.idispatch.server.ui.management;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.masterdetaillayout.MasterDetailLayout;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.function.SerializableRunnable;
import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;
import net.pkhapps.idispatch.server.boundary.DefaultFilter;
import net.pkhapps.idispatch.server.boundary.ManagementService;
import net.pkhapps.idispatch.server.entity.AbstractEntity;
import net.pkhapps.idispatch.server.entity.Deactivatable;
import net.pkhapps.idispatch.server.entity.ValidationFailedException;
import net.pkhapps.idispatch.server.ui.ViewTitle;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;

import static java.util.Objects.requireNonNull;

/**
 * Base class for the management views: a master-detail view with the entities in a grid and a form for the selected
 * or new entity in the detail area. Deleted entities are shown read-only, with a button for restoring them.
 */
public abstract class AbstractManagementView<E extends AbstractEntity, S extends ManagementService<E>>
        extends VerticalLayout implements HasDynamicTitle, BeforeLeaveObserver {

    private final S service;
    private final String titleKey;
    private final Grid<E> grid;
    private final MasterDetailLayout masterDetail;
    private final ValueSignal<String> searchTerm = new ValueSignal<>("");
    private final ValueSignal<Boolean> includeDeleted = new ValueSignal<>(false);
    // The entity in the detail area, or null when it is closed. Compared by identity, since entities are equal by ID
    // and the entity returned by a save must replace the one that was edited.
    private final ValueSignal<@Nullable E> editing = new ValueSignal<>(null, (a, b) -> a == b);
    private @Nullable ManagementForm<? extends E> form;

    /**
     * @param titleKey the translation key of the view's title
     */
    public AbstractManagementView(S service, String titleKey) {
        this.service = requireNonNull(service);
        this.titleKey = requireNonNull(titleKey);

        grid = new Grid<>();
        configureGrid(grid);
        grid.setSizeFull();
        grid.addThemeVariants(GridVariant.NO_BORDER);
        grid.setPartNameGenerator(entity -> isDeleted(entity) ? "deleted" : null);
        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.isFromClient()) {
                confirmDiscardChanges(() -> editing.set(event.getValue()), this::showSelection);
            }
        });

        var searchField = new TextField();
        searchField.setPlaceholder(getTranslation("management.search"));
        searchField.setAriaLabel(getTranslation("management.search-label"));
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.bindValue(searchTerm, searchTerm::set);

        // Rarely used, so tucked away in a menu. The button is highlighted while deleted entities are shown.
        var viewOptions = new MenuBar();
        viewOptions.addClassName("view-options");
        viewOptions.bindThemeVariant(MenuBarVariant.PRIMARY, includeDeleted);
        var viewOptionsItem = viewOptions.addItem(VaadinIcon.FILTER.create(),
                getTranslation("management.view-options"));
        viewOptionsItem.setAriaLabel(getTranslation("management.view-options"));
        var includeDeletedItem = viewOptionsItem.getSubMenu().addItem(getTranslation("management.include-deleted"),
                event -> includeDeleted.set(event.getSource().isChecked()));
        includeDeletedItem.setCheckable(true);

        var refresh = new Button(VaadinIcon.REFRESH.create(), event -> refresh());
        refresh.setAriaLabel(getTranslation("common.refresh"));

        setSizeFull();
        setPadding(false);
        setSpacing(false);

        var toolbar = new HorizontalLayout(new ViewTitle(getTranslation(titleKey)));
        toolbar.addToEnd(searchField);
        if (supportsDelete()) {
            toolbar.addToEnd(viewOptions);
        }
        toolbar.addToEnd(refresh, createAddButton());
        toolbar.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        toolbar.setWidthFull();
        toolbar.setPadding(true);
        toolbar.addClassName("toolbar");
        add(toolbar);

        masterDetail = new MasterDetailLayout();
        masterDetail.setSizeFull();
        masterDetail.setExpandMaster(true);
        masterDetail.setDetailSize("24rem");
        masterDetail.setMaster(grid);
        masterDetail.setDetailPlaceholder(new VerticalLayout(new Span(getTranslation("management.no-selection"))));
        masterDetail.addBackdropClickListener(event -> confirmDiscardChanges(() -> editing.set(null), () -> {
        }));
        masterDetail.addDetailEscapePressListener(event -> confirmDiscardChanges(() -> editing.set(null), () -> {
        }));
        addAndExpand(masterDetail);

        Signal.effect(grid, () -> showItems(new DefaultFilter(includeDeleted.get(), searchTerm.get())));
        Signal.effect(masterDetail, () -> showDetail(editing.get()));
    }

    /**
     * Creates the button that opens the form for a new entity. Use {@link #addButton(SerializableSupplier)} for a
     * single kind of entity.
     */
    protected abstract Component createAddButton();

    protected Button addButton(SerializableSupplier<E> newEntity) {
        var add = new Button(getTranslation("management.add"), VaadinIcon.PLUS_CIRCLE.create(),
                event -> addNew(newEntity.get()));
        add.addThemeVariants(ButtonVariant.PRIMARY);
        return add;
    }

    /**
     * Opens the form for the given new entity, after asking the user whether to discard any unsaved changes.
     */
    protected void addNew(E entity) {
        confirmDiscardChanges(() -> editing.set(entity), () -> {
        });
    }

    protected abstract void configureGrid(Grid<E> grid);

    /**
     * Creates the form for the given entity, with the entity already set as its form data object.
     */
    protected abstract ManagementForm<? extends E> createForm(E entity);

    protected abstract String getTitle(E entity);

    protected S getService() {
        return service;
    }

    /**
     * Whether the service supports soft deletion, and so deleting, restoring and showing deleted entities.
     */
    protected boolean supportsDelete() {
        return getService() instanceof ManagementService.SoftDeletable;
    }

    protected boolean canEdit(E entity) {
        return !isDeleted(entity);
    }

    protected boolean canDelete(E entity) {
        return supportsDelete() && !entity.isNew() && !isDeleted(entity);
    }

    protected boolean canRestore(E entity) {
        return supportsDelete() && isDeleted(entity);
    }

    protected boolean isDeleted(E entity) {
        return entity instanceof Deactivatable deactivatable && !deactivatable.isActive();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected void delete(E entity) {
        if (getService() instanceof ManagementService.SoftDeletable<?>) {
            ((ManagementService.SoftDeletable) getService()).delete(entity);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected void restore(E entity) {
        if (getService() instanceof ManagementService.SoftDeletable<?>) {
            ((ManagementService.SoftDeletable) getService()).restore(entity);
        }
    }

    @Override
    public String getPageTitle() {
        return "iDispatch | " + getTranslation(titleKey);
    }

    @Override
    public void beforeLeave(BeforeLeaveEvent event) {
        if (hasChanges()) {
            var action = event.postpone();
            confirmDiscardChanges(action::proceed, action::cancel);
        }
    }

    private void refresh() {
        showItems(new DefaultFilter(includeDeleted.peek(), searchTerm.peek()));
    }

    private void showItems(DefaultFilter filter) {
        grid.setItems(getService().findAll(filter));
        showSelection();
    }

    private void showSelection() {
        var entity = editing.peek();
        grid.asSingleSelect().setValue(entity == null || entity.isNew() ? null : entity);
    }

    private void showDetail(@Nullable E entity) {
        showSelection();
        if (entity == null) {
            form = null;
            masterDetail.setDetail(null);
        } else {
            masterDetail.setDetail(createEditor(entity));
        }
    }

    private Component createEditor(E entity) {
        var editor = createForm(entity);
        editor.setReadOnly(!canEdit(entity));
        if (entity.isNew()) {
            editor.focus();
        }
        form = editor;

        var errors = new Span();
        errors.addClassName("form-errors");
        errors.setVisible(false);

        var layout = new VerticalLayout(new H3(getTitle(entity)), editor, errors);
        layout.addClassName("management-editor");

        var buttons = new HorizontalLayout();
        buttons.setWidthFull();
        if (canEdit(entity)) {
            var save = new Button(getTranslation("common.save"), event -> save(editor, errors));
            save.addThemeVariants(ButtonVariant.PRIMARY);
            save.addClickShortcut(Key.ENTER).listenOn(layout);
            buttons.add(save);
        }
        buttons.add(new Button(getTranslation("common.cancel"), event -> editing.set(null)));
        if (canDelete(entity)) {
            var delete = new Button(getTranslation("management.delete"), event -> {
                delete(entity);
                close();
            });
            delete.addThemeVariants(ButtonVariant.ERROR);
            buttons.addToEnd(delete);
        }
        if (canRestore(entity)) {
            buttons.addToEnd(new Button(getTranslation("management.restore"), event -> {
                restore(entity);
                close();
            }));
        }

        layout.add(buttons);
        return layout;
    }

    private void save(ManagementForm<? extends E> editor, Span errors) {
        editor.getFormDataObject().ifPresent(entity -> {
            try {
                var saved = getService().save(entity);
                refresh();
                editing.set(saved);
            } catch (ValidationFailedException ex) {
                errors.setText(String.join(" ", ex.getViolationMessages(getLocale())));
                errors.setVisible(true);
            } catch (DataIntegrityViolationException ex) {
                var notification = new Notification(getTranslation("management.data-integrity-violation"));
                notification.addThemeVariants(NotificationVariant.WARNING);
                notification.setPosition(Notification.Position.MIDDLE);
                notification.setDuration(3000);
                notification.open();
            }
        });
    }

    private void close() {
        editing.set(null);
        refresh();
    }

    private boolean hasChanges() {
        return form != null && form.hasChanges();
    }

    private void confirmDiscardChanges(SerializableRunnable onDiscard, SerializableRunnable onKeep) {
        if (!hasChanges()) {
            onDiscard.run();
            return;
        }
        var dialog = new ConfirmDialog();
        dialog.setHeader(getTranslation("management.discard-changes.header"));
        dialog.setText(getTranslation("management.discard-changes.text"));
        dialog.setCancelable(true);
        dialog.setCancelText(getTranslation("common.cancel"));
        dialog.setConfirmText(getTranslation("management.discard-changes.confirm"));
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(event -> onDiscard.run());
        dialog.addCancelListener(event -> onKeep.run());
        dialog.open();
    }
}
