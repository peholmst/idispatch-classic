package net.pkhapps.idispatch.server.ui.reports;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.data.binder.Setter;
import com.vaadin.flow.signals.Signal;
import net.pkhapps.idispatch.server.boundary.ReportDTO;
import net.pkhapps.idispatch.server.boundary.ReportResourceDTO;
import net.pkhapps.idispatch.server.boundary.ReportService;
import net.pkhapps.idispatch.server.entity.AssignmentReport;
import net.pkhapps.idispatch.server.entity.AssignmentUrgency;
import net.pkhapps.idispatch.server.entity.Resource;
import net.pkhapps.idispatch.server.entity.UnitReport;
import net.pkhapps.idispatch.server.entity.UserStamp;
import net.pkhapps.idispatch.server.entity.ValidationFailedException;
import net.pkhapps.idispatch.server.ui.Formatters;
import net.pkhapps.idispatch.server.util.UpdateResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

class AssignmentReportPanel extends VerticalLayout {

    private final ReportService reportService;
    private final boolean canWrite;
    private final boolean canReview;
    private final Binder<ReportDTO> binder;
    private final Binder<AssignmentReport> sceneBinder;
    private final List<UnitReportForm> unitForms = new ArrayList<>();
    private final Map<Resource, Details> unitDetails = new HashMap<>();
    private final Grid<ReportResourceDTO> resourcesGrid;
    private final Scroller content;
    private final VerticalLayout units;
    private final Span sceneLastEdit;
    private final Badge reportStatus;
    private final Span closedBy;
    private final Button save;
    private final Button close;
    private final Button reopen;
    private ReportDTO bean;
    private boolean editable;
    private SerializableConsumer<AssignmentReport> reportChangeListener = report -> {
    };
    private boolean loading;
    private boolean unsavedChanges;

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

        binder = new Binder<>();
        var formLayout = new FormLayout();
        formLayout.setWidthFull();

        var number = new TextField();
        number.setWidthFull();
        formLayout.addFormItem(number, "Number");
        binder.forField(number)
                .withConverter(this::noConverter, Objects::toString)
                .bindReadOnly(ReportDTO::getNumber);
        formLayout.getElement().appendChild(ElementFactory.createBr());

        var opened = new TextField();
        opened.setWidthFull();
        formLayout.addFormItem(opened, "Opened");
        binder.forField(opened)
                .withConverter(this::noConverter, Formatters::formatDateTime)
                .bindReadOnly(ReportDTO::getOpened);

        var closed = new TextField();
        closed.setWidthFull();
        formLayout.addFormItem(closed, "Closed");
        binder.forField(closed)
                .withConverter(this::noConverter, Formatters::formatDateTime)
                .bindReadOnly(ReportDTO::getClosed);

        var urgency = new TextField();
        urgency.setWidthFull();
        formLayout.addFormItem(urgency, "Urgency");
        binder.forField(urgency)
                .withConverter(this::noConverter, AssignmentUrgency::toString)
                .bindReadOnly(ReportDTO::getUrgency);

        var type = new TextField();
        type.setWidthFull();
        formLayout.addFormItem(type, "Type");
        binder.forField(type)
                .withConverter(this::noConverter, Formatters::formatAssignmentType)
                .bindReadOnly(ReportDTO::getType);

        var description = new TextArea();
        description.setWidthFull();
        var descriptionItem = formLayout.addFormItem(description, "Description");
        formLayout.setColspan(descriptionItem, 2);
        binder.forField(description)
                .bindReadOnly(ReportDTO::getDescription);

        var municipality = new TextField();
        municipality.setWidthFull();
        formLayout.addFormItem(municipality, "Municipality");
        binder.forField(municipality)
                .withConverter(this::noConverter, Formatters::formatMunicipality)
                .bindReadOnly(ReportDTO::getMunicipality);

        var address = new TextField();
        address.setWidthFull();
        formLayout.addFormItem(address, "Address");
        binder.forField(address)
                .bindReadOnly(ReportDTO::getAddress);

        resourcesGrid = new Grid<>();
        resourcesGrid.setWidthFull();
        resourcesGrid.setAllRowsVisible(true);
        resourcesGrid.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_ROW_STRIPES);
        resourcesGrid.setSelectionMode(Grid.SelectionMode.NONE);

        resourcesGrid.addColumn(ReportResourceDTO::getCallSign).setHeader("Resource");
        resourcesGrid.addColumn(dto -> Formatters.formatTime(dto.getDispatched())).setHeader("Dispatched");
        resourcesGrid.addColumn(dto -> Formatters.formatTime(dto.getEnRoute())).setHeader("En route");
        resourcesGrid.addColumn(dto -> Formatters.formatTime(dto.getOnScene())).setHeader("On scene");
        resourcesGrid.addColumn(dto -> Formatters.formatTime(dto.getAvailable())).setHeader("Available");
        resourcesGrid.addColumn(dto -> Formatters.formatTime(dto.getAtStation())).setHeader("At station");

        sceneBinder = new Binder<>();
        var sceneForm = new FormLayout();
        sceneForm.setWidthFull();
        sceneForm.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2), new FormLayout.ResponsiveStep("600px", 4));

        var situationDevelopment = createTextArea("How did the situation develop?");
        sceneForm.add(situationDevelopment, 4);
        sceneBinder.forField(situationDevelopment)
                .bind(AssignmentReport::getSituationDevelopment, AssignmentReport::setSituationDevelopment);

        sceneForm.add(
                createCountField("How many people were involved?", AssignmentReport::getPeopleInvolved, AssignmentReport::setPeopleInvolved),
                createCountField("How many lightly injured?", AssignmentReport::getLightlyInjured, AssignmentReport::setLightlyInjured),
                createCountField("How many seriously injured?", AssignmentReport::getSeriouslyInjured, AssignmentReport::setSeriouslyInjured),
                createCountField("How many deceased?", AssignmentReport::getDeceased, AssignmentReport::setDeceased)
        );

        sceneBinder.addValueChangeListener(event -> markChanged());
        sceneLastEdit = createLastEditSpan();

        units = new VerticalLayout();
        units.setPadding(false);

        reportStatus = new Badge();
        closedBy = createLastEditSpan();
        var statusRow = new HorizontalLayout(reportStatus, closedBy);
        statusRow.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.BASELINE);

        save = new Button("Save Report", event -> save());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        close = new Button("Close Report", VaadinIcon.LOCK.create(), event -> close());
        close.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        reopen = new Button("Re-open Report", VaadinIcon.UNLOCK.create(), event -> reopen());
        var actions = new HorizontalLayout(save, close, reopen);

        var contentLayout = new VerticalLayout(statusRow, formLayout, resourcesGrid, new H3("What happened on scene"),
                sceneLastEdit, sceneForm, new H3("Responding units"), units, actions);
        content = new Scroller(contentLayout);
        content.setSizeFull();
        add(content);

        setBean(null);
    }

    private static Span createLastEditSpan() {
        var span = new Span();
        span.getStyle().set("color", "var(--lumo-secondary-text-color)").set("font-size", "var(--lumo-font-size-s)");
        return span;
    }

    private static String formatLastEdit(UserStamp lastEdit) {
        return lastEdit == null ? "Not filled in yet"
                : "Last edited by " + lastEdit.getDisplayName() + " on " + Formatters.formatDateTime(lastEdit.getTimestamp());
    }

    private void markChanged() {
        if (!loading && editable) {
            setUnsavedChanges(true);
        }
    }

    /**
     * Returns whether the user has changed the report without saving it.
     */
    boolean hasUnsavedChanges() {
        return unsavedChanges;
    }

    private void setUnsavedChanges(boolean unsavedChanges) {
        if (this.unsavedChanges != unsavedChanges) {
            this.unsavedChanges = unsavedChanges;
            // Makes the browser ask before the tab is closed or reloaded; navigation inside the app is guarded by the view
            getElement().executeJs("window.onbeforeunload = $0 ? (e) => { e.preventDefault(); e.returnValue = ''; } : null",
                    unsavedChanges);
        }
    }

    private TextArea createTextArea(String label) {
        var textArea = new TextArea(label);
        textArea.setWidthFull();
        textArea.setMinRows(3);
        textArea.setMaxLength(AssignmentReport.MAX_TEXT_LENGTH);
        // Send the text while the user types, so that closing the tab right after typing still warns about it
        textArea.setValueChangeMode(ValueChangeMode.LAZY);
        return textArea;
    }

    private IntegerField createCountField(String label, ValueProvider<AssignmentReport, Integer> getter, Setter<AssignmentReport, Integer> setter) {
        var field = new IntegerField(label);
        field.setWidthFull();
        field.setMin(0);
        field.setStepButtonsVisible(true);
        sceneBinder.forField(field)
                .withValidator(value -> value == null || value >= 0, "Cannot be negative")
                .bind(getter, setter);
        return field;
    }

    /**
     * Sets the listener that is told when the report shown has been saved, closed or re-opened, or when a save or a
     * review found that someone else had changed it.
     */
    void setReportChangeListener(SerializableConsumer<AssignmentReport> reportChangeListener) {
        this.reportChangeListener = reportChangeListener;
    }

    private void showChangedReport(AssignmentReport report) {
        setBean(bean.withReport(report));
        reportChangeListener.accept(report);
    }

    /**
     * Shows the given report, throwing away any unsaved changes to the previous one.
     */
    public void setBean(ReportDTO bean) {
        loading = true;
        try {
            doSetBean(bean);
        } finally {
            loading = false;
        }
        setUnsavedChanges(false);
    }

    private void doSetBean(ReportDTO bean) {
        var previousBean = this.bean;
        this.bean = bean;
        content.setVisible(bean != null);

        // Nobody can change a closed report, not even a writer; only a reviewer can re-open it
        var reportClosed = bean != null && bean.getReport().isClosed();
        editable = canWrite && bean != null && !reportClosed;
        sceneBinder.setReadOnly(!editable);
        save.setVisible(editable);
        close.setVisible(canReview && bean != null && !reportClosed);
        reopen.setVisible(canReview && reportClosed);
        ReportStatus.of(reportClosed).applyTo(reportStatus);
        closedBy.setText(reportClosed ? "Closed by " + bean.getReport().getClosed().getDisplayName() + " on "
                + Formatters.formatDateTime(bean.getReport().getClosed().getTimestamp()) : "");

        binder.setBean(bean);
        resourcesGrid.setItems(bean == null ? Collections.emptyList() : bean.getResources());
        sceneBinder.setBean(bean == null ? null : bean.getReport());
        sceneLastEdit.setText(bean == null ? "" : formatLastEdit(bean.getReport().getLastEdit()));

        // Showing the same assignment again, such as after saving, keeps the units the user had opened open
        var openedUnits = previousBean != null && bean != null && previousBean.getNumber().equals(bean.getNumber())
                ? unitDetails.entrySet().stream().filter(entry -> entry.getValue().isOpened()).map(Map.Entry::getKey).toList()
                : null;
        units.removeAll();
        unitForms.clear();
        unitDetails.clear();
        if (bean != null) {
            var respondingResources = bean.getRespondingResources();
            if (respondingResources.isEmpty()) {
                units.add(new Span("No units responded to this assignment"));
            }
            respondingResources.forEach(resource -> {
                var unitReport = bean.getReport().getUnitReport(resource);
                var unitForm = new UnitReportForm(resource, unitReport, editable);
                unitForm.binder.addValueChangeListener(event -> markChanged());
                unitForms.add(unitForm);

                var details = new Details(createUnitSummary(resource, unitReport, unitForm.status()), unitForm);
                details.setWidthFull();
                // A writer usually works on their own unit only, so every unit starts closed and stays out of the way
                details.setOpened(openedUnits != null && openedUnits.contains(resource));
                unitDetails.put(resource, details);
                units.add(details);
            });
        }
    }

    private static Component createUnitSummary(Resource resource, UnitReport unitReport, Signal<UnitReportStatus> status) {
        var callSign = new Span(resource.getCallSign());
        callSign.getStyle().set("font-weight", "600");
        // In the summary, so it shows whether the unit is open or not
        var badge = new Badge();
        badge.bindText(status.map(UnitReportStatus::getLabel));
        badge.bindThemeName(BadgeVariant.SUCCESS.getVariantName(), status.map(UnitReportStatus.FILLED_IN::equals));
        badge.bindThemeName(BadgeVariant.WARNING.getVariantName(), status.map(UnitReportStatus.PARTLY_FILLED_IN::equals));
        var lastEdit = createLastEditSpan();
        lastEdit.setText(formatLastEdit(unitReport.getLastEdit()));
        var summary = new HorizontalLayout(callSign, badge, lastEdit);
        summary.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.BASELINE);
        return summary;
    }

    private void save() {
        if (bean == null || !editable) {
            return;
        }
        var valid = sceneBinder.validate().isOk();
        for (var unitForm : unitForms) {
            if (!unitForm.binder.validate().isOk()) {
                // Don't hide the error in a closed unit
                unitDetails.get(unitForm.getResource()).setOpened(true);
                valid = false;
            }
        }
        if (!valid) {
            return;
        }
        try {
            var result = reportService.saveReport(bean.getReport());
            if (result instanceof UpdateResult.NoChange<AssignmentReport> noChange) {
                noChange.getData().ifPresent(this::showChangedReport);
                showNotification("A reviewer closed this report while you were writing, so your changes couldn't be saved.",
                        NotificationVariant.LUMO_WARNING);
            } else if (result instanceof UpdateResult.Conflict<AssignmentReport> conflict && conflict.getConflictingData() != null) {
                showChangedReport(conflict.getConflictingData());
                showNotification("Someone else saved this report while you were writing yours. Their version is shown now, so add your changes again.",
                        NotificationVariant.LUMO_WARNING);
            } else {
                result.getData().ifPresent(this::showChangedReport);
                showNotification("Report saved", NotificationVariant.LUMO_SUCCESS);
            }
        } catch (ValidationFailedException ex) {
            showNotification(String.join("\n", ex.getViolationMessages(Locale.getDefault())), NotificationVariant.LUMO_ERROR);
        }
    }

    private void close() {
        if (bean == null || !canReview) {
            return;
        }
        if (unsavedChanges) {
            // Only possible for a reviewer who also writes reports
            showNotification("Save or throw away your changes before closing the report.", NotificationVariant.LUMO_WARNING);
            return;
        }
        showReviewResult(reportService.closeReport(bean.getReport()), "Report closed");
    }

    private void reopen() {
        if (bean == null || !canReview) {
            return;
        }
        showReviewResult(reportService.reopenReport(bean.getReport()), "Report re-opened");
    }

    private void showReviewResult(UpdateResult<AssignmentReport> result, String successText) {
        if (result instanceof UpdateResult.Conflict<AssignmentReport> conflict) {
            if (conflict.getConflictingData() != null) {
                showChangedReport(conflict.getConflictingData());
            }
            showNotification("This report has changed since you opened it. The latest version is shown now, so check it again.",
                    NotificationVariant.LUMO_WARNING);
        } else {
            result.getData().ifPresent(this::showChangedReport);
            showNotification(successText, NotificationVariant.LUMO_SUCCESS);
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
        if (unsavedChanges) {
            detachEvent.getUI().getPage().executeJs("window.onbeforeunload = null");
        }
    }

    private <M, V> M noConverter(V presentation) {
        throw new UnsupportedOperationException("Conversion to model is not supported");
    }

    /**
     * How much of a unit's report has been answered, judging by what is in the form right now, saved or not.
     */
    enum UnitReportStatus {
        NOT_FILLED_IN("Not filled in"),
        PARTLY_FILLED_IN("Partly filled in"),
        FILLED_IN("Filled in");

        private final String label;

        UnitReportStatus(String label) {
            this.label = label;
        }

        String getLabel() {
            return label;
        }

        static UnitReportStatus of(String situationOnArrival, String actionsTaken) {
            var answered = Stream.of(situationOnArrival, actionsTaken)
                    .filter(answer -> answer != null && !answer.isBlank())
                    .count();
            return answered == 2 ? FILLED_IN : answered == 1 ? PARTLY_FILLED_IN : NOT_FILLED_IN;
        }
    }

    /**
     * What one responding unit found and did on scene.
     */
    static class UnitReportForm extends VerticalLayout {

        private final Resource resource;
        private final Binder<UnitReport> binder = new Binder<>();
        private final Signal<UnitReportStatus> status;

        UnitReportForm(Resource resource, UnitReport unitReport, boolean editable) {
            this.resource = resource;
            setPadding(false);
            setSpacing(false);

            var situationOnArrival = new TextArea("What was the situation like when you arrived?");
            var actionsTaken = new TextArea("What actions did you take?");
            for (var textArea : List.of(situationOnArrival, actionsTaken)) {
                textArea.setWidthFull();
                textArea.setMinRows(3);
                textArea.setMaxLength(AssignmentReport.MAX_TEXT_LENGTH);
                textArea.setValueChangeMode(ValueChangeMode.LAZY);
            }
            var situationOnArrivalBinding = binder.forField(situationOnArrival)
                    .bind(UnitReport::getSituationOnArrival, UnitReport::setSituationOnArrival);
            var actionsTakenBinding = binder.forField(actionsTaken)
                    .bind(UnitReport::getActionsTaken, UnitReport::setActionsTaken);
            status = Signal.computed(() -> UnitReportStatus.of(situationOnArrivalBinding.valueSignal().get(),
                    actionsTakenBinding.valueSignal().get()));
            binder.setBean(unitReport);
            binder.setReadOnly(!editable);

            add(situationOnArrival, actionsTaken);
        }

        Resource getResource() {
            return resource;
        }

        Signal<UnitReportStatus> status() {
            return status;
        }
    }
}
