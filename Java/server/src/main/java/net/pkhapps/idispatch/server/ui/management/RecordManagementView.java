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
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;
import net.pkhapps.idispatch.server.shared.management.DefaultFilter;
import net.pkhapps.idispatch.server.ui.ViewTitle;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * Base class for the management views of master data stored as immutable records: a master-detail view with the list
 * items in a grid and a {@link RecordForm} for the selected or new record in the detail area. Deleted records are shown
 * read-only, with a button for restoring them.
 *
 * @param <I> the list item shown in the grid, flattened to what the grid shows
 * @param <R> the record that the form edits
 * @param <D> what the form creates: the details of a record, from which a new record is created or an existing one
 *            changed
 */
public abstract class RecordManagementView<I, R, D> extends VerticalLayout
        implements HasDynamicTitle, BeforeLeaveObserver {

    /**
     * What the detail area shows: a new record being added, or an existing one.
     */
    private sealed interface Editing<R> {
    }

    /**
     * @param kind which kind of record, for views that add more than one; see {@link #startAdding(Object)}
     */
    private record Adding<R>(@Nullable Object kind) implements Editing<R> {
    }

    private record Existing<R>(R record) implements Editing<R> {
    }

    private final String titleKey;
    private final Grid<I> grid;
    private final MasterDetailLayout masterDetail;
    private final ValueSignal<String> searchTerm = new ValueSignal<>("");
    private final ValueSignal<Boolean> includeDeleted = new ValueSignal<>(false);
    private final Signal<DefaultFilter> filter = Signal.computed(
            () -> new DefaultFilter(includeDeleted.get(), searchTerm.get()));
    // The list items as last loaded with the filter; the grid shows them
    private final ValueSignal<List<I>> items = new ValueSignal<>(List.of());
    // Compared by identity, so that opening the same record again, or adding again, gives a fresh form
    private final ValueSignal<@Nullable Editing<R>> editing = new ValueSignal<>(null, (a, b) -> a == b);
    private @Nullable RecordForm<D> form;

    /**
     * @param titleKey the translation key of the view's title
     */
    protected RecordManagementView(String titleKey) {
        this.titleKey = requireNonNull(titleKey);

        grid = new Grid<>();
        configureGrid(grid);
        grid.setSizeFull();
        grid.addThemeVariants(GridVariant.NO_BORDER);
        grid.setPartNameGenerator(item -> isItemActive(item) ? null : "deleted");
        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.isFromClient()) {
                confirmDiscardChanges(() -> open(event.getValue()), this::showSelection);
            }
        });

        var searchField = new TextField();
        searchField.setPlaceholder(getTranslation("management.search"));
        searchField.setAriaLabel(getTranslation("management.search-label"));
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.bindValue(searchTerm, searchTerm::set);

        // Rarely used, so tucked away in a menu. The button is highlighted while deleted records are shown.
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
        toolbar.addToEnd(searchField, viewOptions, refresh, createAddButton());
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

        Signal.effect(grid, () -> loadItems(filter.get()));
        // Setting the items clears the selection, so the record being edited is selected again after
        Signal.effect(grid, () -> {
            grid.setItems(items.get());
            showSelection();
        });
        Signal.effect(masterDetail, () -> showDetail(editing.get()));
    }

    protected abstract void configureGrid(Grid<I> grid);

    /**
     * Creates the button that starts adding a record. A view that adds more than one kind of record overrides this
     * with a menu whose items call {@link #startAdding(Object)} with the kind.
     */
    protected Component createAddButton() {
        var add = new Button(getTranslation("management.add"), VaadinIcon.PLUS_CIRCLE.create(),
                event -> startAdding(null));
        add.addThemeVariants(ButtonVariant.PRIMARY);
        return add;
    }

    /**
     * Shows an empty form for a new record of the given kind, once the user agrees to discard any changes.
     *
     * @param kind passed to {@link #createFormToAdd(Object)} and {@link #getTitleToAdd(Object)}
     */
    protected final void startAdding(@Nullable Object kind) {
        confirmDiscardChanges(() -> editing.set(new Adding<>(kind)), () -> {
        });
    }

    /**
     * Creates the empty form for a new record of the given kind. Calls {@link #createForm(Object)} unless overridden.
     */
    protected RecordForm<D> createFormToAdd(@Nullable Object kind) {
        return createForm(null);
    }

    /**
     * Returns the title of the detail area for a new record of the given kind. Calls {@link #getTitle(Object)} unless
     * overridden.
     */
    protected String getTitleToAdd(@Nullable Object kind) {
        return getTitle(null);
    }

    protected abstract List<I> findItems(DefaultFilter filter);

    /**
     * Loads the record of the list item, or returns empty if it no longer exists.
     */
    protected abstract Optional<R> findRecord(I item);

    /**
     * Whether the list item is the given record.
     */
    protected abstract boolean isItemOf(I item, R record);

    protected abstract boolean isItemActive(I item);

    protected abstract boolean isRecordActive(R record);

    /**
     * Creates the form, showing the given record or empty for a new one.
     */
    protected abstract RecordForm<D> createForm(@Nullable R record);

    /**
     * Saves what the form created, as a new record or as a change of the given one.
     *
     * @return the record as saved
     */
    protected abstract R save(@Nullable R record, D details);

    protected abstract void delete(R record);

    protected abstract void restore(R record);

    /**
     * Returns the title of the detail area for the given record, or for a new one.
     */
    protected abstract String getTitle(@Nullable R record);

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

    private void open(@Nullable I item) {
        editing.set(item == null ? null : findRecord(item).<Editing<R>>map(Existing::new).orElse(null));
        if (item != null && editing.peek() == null) {
            // Removed by someone else since the list was loaded
            refresh();
        }
    }

    /**
     * Loads the list items again with the current filter, to show changes made since they were loaded.
     */
    private void refresh() {
        loadItems(filter.peek());
    }

    private void loadItems(DefaultFilter filter) {
        items.set(findItems(filter));
    }

    private void showSelection() {
        var selected = editing.peek() instanceof Existing<R>(var record)
                ? grid.getListDataView().getItems().filter(item -> isItemOf(item, record)).findFirst().orElse(null)
                : null;
        grid.asSingleSelect().setValue(selected);
    }

    private void showDetail(@Nullable Editing<R> editing) {
        showSelection();
        switch (editing) {
            case null -> {
                form = null;
                masterDetail.setDetail(null);
            }
            case Adding<R>(var kind) -> masterDetail.setDetail(createEditor(null, kind));
            case Existing<R>(var record) -> masterDetail.setDetail(createEditor(record, null));
        }
    }

    private Component createEditor(@Nullable R record, @Nullable Object kind) {
        var editor = record == null ? createFormToAdd(kind) : createForm(record);
        var editable = record == null || isRecordActive(record);
        editor.setReadOnly(!editable);
        if (record == null) {
            editor.focus();
        }
        form = editor;

        var layout = new VerticalLayout(new H3(record == null ? getTitleToAdd(kind) : getTitle(record)), editor);
        layout.addClassName("management-editor");

        var buttons = new HorizontalLayout();
        buttons.setWidthFull();
        if (editable) {
            var save = new Button(getTranslation("common.save"), event -> save(editor, record));
            save.addThemeVariants(ButtonVariant.PRIMARY);
            save.addClickShortcut(Key.ENTER).listenOn(layout);
            buttons.add(save);
        }
        buttons.add(new Button(getTranslation("common.cancel"), event -> editing.set(null)));
        if (record != null && isRecordActive(record)) {
            var delete = new Button(getTranslation("management.delete"), event -> {
                delete(record);
                close();
            });
            delete.addThemeVariants(ButtonVariant.ERROR);
            buttons.addToEnd(delete);
        }
        if (record != null && !isRecordActive(record)) {
            buttons.addToEnd(new Button(getTranslation("management.restore"), event -> {
                restore(record);
                close();
            }));
        }

        layout.add(buttons);
        return layout;
    }

    private void save(RecordForm<D> editor, @Nullable R record) {
        editor.validate().ifPresent(details -> {
            try {
                var saved = save(record, details);
                refresh();
                editing.set(new Existing<>(saved));
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
