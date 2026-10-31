package net.pkhapps.idispatch.server.report;

import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;
import net.pkhapps.idispatch.server.assignmenttype.AssignmentTypeFormatters;
import net.pkhapps.idispatch.server.municipality.MunicipalityFormatters;
import net.pkhapps.idispatch.server.resource.ResourceId;
import net.pkhapps.idispatch.server.shared.UpdateResult;
import net.pkhapps.idispatch.server.ui.Formatters;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.google.common.base.Strings.nullToEmpty;
import static java.util.Objects.requireNonNull;

/**
 * Shows the report of a closed assignment: what the assignment was and when each resource got where, read-only, and a
 * {@link ReportForm} for what happened on scene, which writers fill in while the report is open. Reviewers close and
 * re-open it.
 * <p>
 * The report shown is a signal that the view owns: the view sets it when the user opens an assignment, and the panel
 * replaces it with the saved version whenever the user saves, closes or re-opens the report.
 */
class AssignmentReportPanel extends VerticalLayout {

    private final ReportService reportService;
    private final boolean canWrite;
    private final boolean canReview;
    private final ValueSignal<@Nullable ReportDTO> shown;
    private final Signal<Boolean> editable;
    private final Div formHolder = new Div();
    private @Nullable ReportForm form;
    // The number of the assignment whose report the form shows
    private @Nullable Long formNumber;
    private SerializableConsumer<AssignmentReport> reportChangeListener = report -> {
    };

    /**
     * @param shown     the report to show, or {@code null} for none
     * @param canWrite  whether the user may fill in what happened on scene while the report is open
     * @param canReview whether the user may close and re-open the report
     */
    AssignmentReportPanel(ReportService reportService, ValueSignal<@Nullable ReportDTO> shown, boolean canWrite,
                          boolean canReview) {
        this.reportService = reportService;
        this.shown = shown;
        this.canWrite = canWrite;
        this.canReview = canReview;
        setSizeFull();
        setPadding(false);

        // Nobody can change a closed report, not even a writer; only a reviewer can re-open it
        var reportClosed = shown.map(bean -> bean != null && bean.getReport().isClosed());
        // The signals are read first: a binding must read a signal even when the user's role decides alone
        editable = Signal.computed(() -> shown.get() != null && !reportClosed.get() && canWrite);

        var formLayout = new FormLayout();
        formLayout.setWidthFull();
        readOnlyField(formLayout, "report.number", bean -> bean.getNumber().toString());
        formLayout.getElement().appendChild(ElementFactory.createBr());
        readOnlyField(formLayout, "report.opened", bean -> Formatters.formatDateTime(bean.getOpened()));
        readOnlyField(formLayout, "report.closed", bean -> Formatters.formatDateTime(bean.getClosed()));
        readOnlyField(formLayout, "assignment.urgency", bean -> bean.getUrgency().toString());
        readOnlyField(formLayout, "assignment.type", bean -> AssignmentTypeFormatters.formatAssignmentType(bean.getType()));
        var description = new TextArea();
        description.setWidthFull();
        description.setReadOnly(true);
        description.bindValue(show(ReportDTO::getDescription), null);
        formLayout.setColspan(formLayout.addFormItem(description, getTranslation("assignment.description")), 2);
        readOnlyField(formLayout, "assignment.municipality",
                bean -> MunicipalityFormatters.formatMunicipality(bean.getMunicipality()));
        readOnlyField(formLayout, "assignment.address", ReportDTO::getAddress);

        var resourcesGrid = new Grid<ReportResourceDTO>();
        resourcesGrid.setWidthFull();
        resourcesGrid.setAllRowsVisible(true);
        resourcesGrid.addThemeVariants(GridVariant.ROW_STRIPES);
        resourcesGrid.setSelectionMode(Grid.SelectionMode.NONE);
        resourcesGrid.addColumn(ReportResourceDTO::getCallSign).setHeader(getTranslation("dispatch.resource"));
        resourcesGrid.addColumn(dto -> Formatters.formatTime(dto.getDispatched())).setHeader(getTranslation("resource-state.DISPATCHED"));
        resourcesGrid.addColumn(dto -> Formatters.formatTime(dto.getEnRoute())).setHeader(getTranslation("resource-state.EN_ROUTE"));
        resourcesGrid.addColumn(dto -> Formatters.formatTime(dto.getOnScene())).setHeader(getTranslation("resource-state.ON_SCENE"));
        resourcesGrid.addColumn(dto -> Formatters.formatTime(dto.getAvailable())).setHeader(getTranslation("resource-state.AVAILABLE"));
        resourcesGrid.addColumn(dto -> Formatters.formatTime(dto.getAtStation())).setHeader(getTranslation("resource-state.AT_STATION"));
        Signal.effect(resourcesGrid, () -> {
            var bean = shown.get();
            resourcesGrid.setItems(bean == null ? List.of() : bean.getResources());
        });

        var reportStatus = new Badge();
        ReportStatus.bind(reportStatus, reportClosed.map(ReportStatus::of));
        var closedBy = ReportForm.createLastEditSpan();
        closedBy.bindText(shown.map(bean -> {
            var closed = bean == null ? null : bean.getReport().closed();
            return closed == null ? "" : getTranslation("report.closed-by", closed.displayName(),
                    Formatters.formatDateTime(closed.timestamp()));
        }));
        var statusRow = new HorizontalLayout(reportStatus, closedBy);
        statusRow.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.BASELINE);

        var save = new Button(getTranslation("report.save"), event -> save());
        save.addThemeVariants(ButtonVariant.PRIMARY);
        save.bindVisible(editable);
        var close = new Button(getTranslation("report.close"), VaadinIcon.LOCK.create(), event -> close());
        close.addThemeVariants(ButtonVariant.SUCCESS);
        close.bindVisible(Signal.computed(() -> shown.get() != null && !reportClosed.get() && canReview));
        var reopen = new Button(getTranslation("report.reopen"), VaadinIcon.UNLOCK.create(), event -> reopen());
        reopen.bindVisible(reportClosed.map(closed -> closed && canReview));
        var actions = new HorizontalLayout(save, close, reopen);

        formHolder.setWidthFull();
        var contentLayout = new VerticalLayout(statusRow, formLayout, resourcesGrid, formHolder, actions);
        var content = new Scroller(contentLayout);
        content.setSizeFull();
        content.bindVisible(shown.map(Objects::nonNull));
        add(content);

        Signal.effect(formHolder, () -> showForm(shown.get()));
    }

    /**
     * Returns a signal of what the field shows of the report, or of an empty string while no report is shown.
     */
    private Signal<String> show(SerializableFunction<ReportDTO, @Nullable String> part) {
        return shown.map(bean -> bean == null ? "" : nullToEmpty(part.apply(bean)));
    }

    private void readOnlyField(FormLayout formLayout, String labelKey,
                               SerializableFunction<ReportDTO, @Nullable String> part) {
        var field = new TextField();
        field.setWidthFull();
        field.setReadOnly(true);
        field.bindValue(show(part), null);
        formLayout.addFormItem(field, getTranslation(labelKey));
    }

    /**
     * Replaces the form with one for the given report, throwing away any unsaved changes to the previous one. Showing
     * the same assignment again, such as after saving, keeps the units the user had opened open.
     */
    private void showForm(@Nullable ReportDTO bean) {
        var openedUnits = form != null && bean != null && bean.getNumber().equals(formNumber)
                ? form.openedUnits() : Set.<ResourceId>of();
        formHolder.removeAll();
        form = null;
        formNumber = null;
        if (bean == null) {
            warnBeforeUnload(false);
            return;
        }
        // The form reads its own signals while it is built, which must not make this effect depend on them
        var shownForm = Signal.untracked(() -> new ReportForm(bean.getReport(), bean.getRespondingResources(),
                openedUnits));
        shownForm.setReadOnly(!editable.get());
        formHolder.add(shownForm);
        form = shownForm;
        formNumber = bean.getNumber();
        var changed = shownForm.changed();
        Signal.effect(shownForm, () -> warnBeforeUnload(changed.get()));
    }

    /**
     * Returns whether the user has changed the report without saving it. Changing a field back is no change.
     */
    boolean hasUnsavedChanges() {
        return form != null && form.hasChanges();
    }

    /**
     * Sets the listener that is told when the report shown has been saved, closed or re-opened, or when a save or a
     * review found that someone else had changed it.
     */
    void setReportChangeListener(SerializableConsumer<AssignmentReport> reportChangeListener) {
        this.reportChangeListener = reportChangeListener;
    }

    private void showChangedReport(AssignmentReport report) {
        shown.set(requireNonNull(shown.peek()).withReport(report));
        reportChangeListener.accept(report);
    }

    /**
     * Makes the browser ask before the tab is closed or reloaded; navigation inside the app is guarded by the view.
     */
    private void warnBeforeUnload(boolean warns) {
        getElement().executeJs("window.onbeforeunload = $0 ? (e) => { e.preventDefault(); e.returnValue = ''; } : null",
                warns);
    }

    private void save() {
        var bean = shown.peek();
        if (bean == null || form == null || !editable.peek()) {
            return;
        }
        var draft = form.validate();
        if (draft.isEmpty()) {
            var casualtiesError = form.casualtiesError();
            if (casualtiesError != null) {
                showNotification(casualtiesError, NotificationVariant.ERROR);
            }
            return;
        }
        var result = reportService.saveReport(bean.getReport(), draft.get());
        if (result instanceof UpdateResult.NoChange<AssignmentReport> noChange) {
            noChange.getData().ifPresent(this::showChangedReport);
            showNotification(getTranslation("report.save.closed-meanwhile"), NotificationVariant.WARNING);
        } else if (result instanceof UpdateResult.Conflict<AssignmentReport> conflict
                && conflict.getConflictingData() != null) {
            showChangedReport(conflict.getConflictingData());
            showNotification(getTranslation("report.save.conflict"), NotificationVariant.WARNING);
        } else {
            result.getData().ifPresent(this::showChangedReport);
            showNotification(getTranslation("report.saved"), NotificationVariant.SUCCESS);
        }
    }

    private void close() {
        var bean = shown.peek();
        if (bean == null || !canReview) {
            return;
        }
        if (hasUnsavedChanges()) {
            // Only possible for a reviewer who also writes reports
            showNotification(getTranslation("report.close.unsaved-changes"), NotificationVariant.WARNING);
            return;
        }
        showReviewResult(reportService.closeReport(bean.getReport()), getTranslation("report.closed-notification"));
    }

    private void reopen() {
        var bean = shown.peek();
        if (bean == null || !canReview) {
            return;
        }
        showReviewResult(reportService.reopenReport(bean.getReport()), getTranslation("report.reopened-notification"));
    }

    private void showReviewResult(UpdateResult<AssignmentReport> result, String successText) {
        if (result instanceof UpdateResult.Conflict<AssignmentReport> conflict) {
            if (conflict.getConflictingData() != null) {
                showChangedReport(conflict.getConflictingData());
            }
            showNotification(getTranslation("report.review.conflict"), NotificationVariant.WARNING);
        } else {
            result.getData().ifPresent(this::showChangedReport);
            showNotification(successText, NotificationVariant.SUCCESS);
        }
    }

    private void showNotification(String text, NotificationVariant variant) {
        var notification = new Notification(text, 3000, Notification.Position.MIDDLE);
        notification.addThemeVariants(variant);
        notification.open();
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        detachEvent.getUI().getPage().executeJs("window.onbeforeunload = null");
    }
}
