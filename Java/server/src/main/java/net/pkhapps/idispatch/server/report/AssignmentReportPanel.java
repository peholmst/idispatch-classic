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
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.signals.Signal;
import net.pkhapps.idispatch.server.assignmenttype.AssignmentTypeFormatters;
import net.pkhapps.idispatch.server.municipality.MunicipalityFormatters;
import net.pkhapps.idispatch.server.resource.ResourceId;
import net.pkhapps.idispatch.server.shared.UpdateResult;
import net.pkhapps.idispatch.server.ui.Formatters;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;

import static com.google.common.base.Strings.nullToEmpty;
import static java.util.Objects.requireNonNull;

/**
 * Shows the report of a closed assignment: what the assignment was and when each resource got where, read-only, and a
 * {@link ReportForm} for what happened on scene, which writers fill in while the report is open. Reviewers close and
 * re-open it.
 */
class AssignmentReportPanel extends VerticalLayout {

    private final ReportService reportService;
    private final boolean canWrite;
    private final boolean canReview;
    private final TextField number;
    private final TextField opened;
    private final TextField closed;
    private final TextField urgency;
    private final TextField type;
    private final TextArea description;
    private final TextField municipality;
    private final TextField address;
    private final Grid<ReportResourceDTO> resourcesGrid;
    private final Scroller content;
    private final Div formHolder = new Div();
    private final Badge reportStatus;
    private final Span closedBy;
    private final Button save;
    private final Button close;
    private final Button reopen;
    private @Nullable ReportDTO bean;
    private @Nullable ReportForm form;
    private boolean editable;
    private SerializableConsumer<AssignmentReport> reportChangeListener = report -> {
    };
    private boolean warnsBeforeUnload;

    /**
     * @param canWrite  whether the user may fill in what happened on scene while the report is open
     * @param canReview whether the user may close and re-open the report
     */
    AssignmentReportPanel(ReportService reportService, boolean canWrite, boolean canReview) {
        this.reportService = reportService;
        this.canWrite = canWrite;
        this.canReview = canReview;
        setSizeFull();
        setPadding(false);

        var formLayout = new FormLayout();
        formLayout.setWidthFull();
        number = readOnlyField(formLayout, "report.number");
        formLayout.getElement().appendChild(ElementFactory.createBr());
        opened = readOnlyField(formLayout, "report.opened");
        closed = readOnlyField(formLayout, "report.closed");
        urgency = readOnlyField(formLayout, "assignment.urgency");
        type = readOnlyField(formLayout, "assignment.type");
        description = new TextArea();
        description.setWidthFull();
        description.setReadOnly(true);
        formLayout.setColspan(formLayout.addFormItem(description, getTranslation("assignment.description")), 2);
        municipality = readOnlyField(formLayout, "assignment.municipality");
        address = readOnlyField(formLayout, "assignment.address");

        resourcesGrid = new Grid<>();
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

        reportStatus = new Badge();
        closedBy = ReportForm.createLastEditSpan();
        var statusRow = new HorizontalLayout(reportStatus, closedBy);
        statusRow.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.BASELINE);

        save = new Button(getTranslation("report.save"), event -> save());
        save.addThemeVariants(ButtonVariant.PRIMARY);
        close = new Button(getTranslation("report.close"), VaadinIcon.LOCK.create(), event -> close());
        close.addThemeVariants(ButtonVariant.SUCCESS);
        reopen = new Button(getTranslation("report.reopen"), VaadinIcon.UNLOCK.create(), event -> reopen());
        var actions = new HorizontalLayout(save, close, reopen);

        formHolder.setWidthFull();
        var contentLayout = new VerticalLayout(statusRow, formLayout, resourcesGrid, formHolder, actions);
        content = new Scroller(contentLayout);
        content.setSizeFull();
        add(content);

        setBean(null);
    }

    private TextField readOnlyField(FormLayout formLayout, String labelKey) {
        var field = new TextField();
        field.setWidthFull();
        field.setReadOnly(true);
        formLayout.addFormItem(field, getTranslation(labelKey));
        return field;
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
        setBean(requireNonNull(bean).withReport(report));
        reportChangeListener.accept(report);
    }

    /**
     * Shows the given report, throwing away any unsaved changes to the previous one. Showing the same assignment
     * again, such as after saving, keeps the units the user had opened open.
     */
    public void setBean(@Nullable ReportDTO bean) {
        var openedUnits = form != null && this.bean != null && bean != null
                && this.bean.getNumber().equals(bean.getNumber()) ? form.openedUnits() : Set.<ResourceId>of();
        this.bean = bean;
        content.setVisible(bean != null);
        formHolder.removeAll();
        form = null;
        setWarnsBeforeUnload(false);

        // Nobody can change a closed report, not even a writer; only a reviewer can re-open it
        var report = bean == null ? null : bean.getReport();
        var reportClosed = report != null && report.isClosed();
        editable = canWrite && bean != null && !reportClosed;
        save.setVisible(editable);
        close.setVisible(canReview && bean != null && !reportClosed);
        reopen.setVisible(canReview && reportClosed);
        ReportStatus.of(reportClosed).applyTo(reportStatus);
        closedBy.setText(report != null && report.closed() != null
                ? getTranslation("report.closed-by", report.closed().displayName(),
                Formatters.formatDateTime(report.closed().timestamp())) : "");

        number.setValue(bean == null ? "" : bean.getNumber().toString());
        opened.setValue(bean == null ? "" : nullToEmpty(Formatters.formatDateTime(bean.getOpened())));
        closed.setValue(bean == null ? "" : nullToEmpty(Formatters.formatDateTime(bean.getClosed())));
        urgency.setValue(bean == null ? "" : bean.getUrgency().toString());
        type.setValue(bean == null ? "" : nullToEmpty(AssignmentTypeFormatters.formatAssignmentType(bean.getType())));
        description.setValue(bean == null ? "" : bean.getDescription());
        municipality.setValue(bean == null ? ""
                : nullToEmpty(MunicipalityFormatters.formatMunicipality(bean.getMunicipality())));
        address.setValue(bean == null ? "" : bean.getAddress());
        resourcesGrid.setItems(bean == null ? List.of() : bean.getResources());

        if (bean != null) {
            var shown = new ReportForm(bean.getReport(), bean.getRespondingResources(), openedUnits);
            shown.setReadOnly(!editable);
            formHolder.add(shown);
            form = shown;
            var changed = shown.changed();
            Signal.effect(shown, () -> setWarnsBeforeUnload(changed.get()));
        }
    }

    /**
     * Makes the browser ask before the tab is closed or reloaded; navigation inside the app is guarded by the view.
     */
    private void setWarnsBeforeUnload(boolean warns) {
        if (warnsBeforeUnload != warns) {
            warnsBeforeUnload = warns;
            getElement().executeJs("window.onbeforeunload = $0 ? (e) => { e.preventDefault(); e.returnValue = ''; } : null",
                    warns);
        }
    }

    private void save() {
        if (bean == null || form == null || !editable) {
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
        if (warnsBeforeUnload) {
            detachEvent.getUI().getPage().executeJs("window.onbeforeunload = null");
            warnsBeforeUnload = false;
        }
    }
}
