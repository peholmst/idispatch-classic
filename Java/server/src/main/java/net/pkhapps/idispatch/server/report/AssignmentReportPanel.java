package net.pkhapps.idispatch.server.report;

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
import com.vaadin.flow.data.binder.Setter;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.signals.Signal;
import net.pkhapps.idispatch.server.assignment.AssignmentUrgency;
import net.pkhapps.idispatch.server.assignmenttype.AssignmentTypeFormatters;
import net.pkhapps.idispatch.server.municipality.MunicipalityFormatters;
import net.pkhapps.idispatch.server.resource.ResourceId;
import net.pkhapps.idispatch.server.resource.ResourceListItem;
import net.pkhapps.idispatch.server.shared.UpdateResult;
import net.pkhapps.idispatch.server.shared.ValidationFailedException;
import net.pkhapps.idispatch.server.ui.Formatters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static com.google.common.base.Strings.nullToEmpty;

class AssignmentReportPanel extends VerticalLayout {

    private final ReportService reportService;
    private final boolean canWrite;
    private final boolean canReview;
    private final Binder<ReportDTO> binder;
    private final Binder<AssignmentReport> sceneBinder;
    private final List<UnitReportForm> unitForms = new ArrayList<>();
    private final Map<ResourceId, Details> unitDetails = new HashMap<>();
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
        formLayout.addFormItem(number, getTranslation("report.number"));
        binder.forField(number)
                .withConverter(this::noConverter, Objects::toString)
                .bindReadOnly(ReportDTO::getNumber);
        formLayout.getElement().appendChild(ElementFactory.createBr());

        var opened = new TextField();
        opened.setWidthFull();
        formLayout.addFormItem(opened, getTranslation("report.opened"));
        binder.forField(opened)
                .withConverter(this::noConverter, Formatters::formatDateTime)
                .bindReadOnly(ReportDTO::getOpened);

        var closed = new TextField();
        closed.setWidthFull();
        formLayout.addFormItem(closed, getTranslation("report.closed"));
        binder.forField(closed)
                .withConverter(this::noConverter, Formatters::formatDateTime)
                .bindReadOnly(ReportDTO::getClosed);

        var urgency = new TextField();
        urgency.setWidthFull();
        formLayout.addFormItem(urgency, getTranslation("assignment.urgency"));
        binder.forField(urgency)
                .withConverter(this::noConverter, AssignmentUrgency::toString)
                .bindReadOnly(ReportDTO::getUrgency);

        var type = new TextField();
        type.setWidthFull();
        formLayout.addFormItem(type, getTranslation("assignment.type"));
        binder.forField(type)
                .bindReadOnly(dto -> nullToEmpty(AssignmentTypeFormatters.formatAssignmentType(dto.getType())));

        var description = new TextArea();
        description.setWidthFull();
        var descriptionItem = formLayout.addFormItem(description, getTranslation("assignment.description"));
        formLayout.setColspan(descriptionItem, 2);
        binder.forField(description)
                .bindReadOnly(ReportDTO::getDescription);

        var municipality = new TextField();
        municipality.setWidthFull();
        formLayout.addFormItem(municipality, getTranslation("assignment.municipality"));
        binder.forField(municipality)
                .bindReadOnly(dto -> nullToEmpty(MunicipalityFormatters.formatMunicipality(dto.getMunicipality())));

        var address = new TextField();
        address.setWidthFull();
        formLayout.addFormItem(address, getTranslation("assignment.address"));
        binder.forField(address)
                .bindReadOnly(ReportDTO::getAddress);

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

        sceneBinder = new Binder<>();
        var sceneForm = new FormLayout();
        sceneForm.setWidthFull();
        sceneForm.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2), new FormLayout.ResponsiveStep("600px", 4));

        var situationDevelopment = createTextArea(getTranslation("report.situation-development"));
        sceneForm.add(situationDevelopment, 4);
        sceneBinder.forField(situationDevelopment)
                .bind(AssignmentReport::getSituationDevelopment, AssignmentReport::setSituationDevelopment);

        sceneForm.add(
                createCountField(getTranslation("report.people-involved"), AssignmentReport::getPeopleInvolved, AssignmentReport::setPeopleInvolved),
                createCountField(getTranslation("report.lightly-injured"), AssignmentReport::getLightlyInjured, AssignmentReport::setLightlyInjured),
                createCountField(getTranslation("report.seriously-injured"), AssignmentReport::getSeriouslyInjured, AssignmentReport::setSeriouslyInjured),
                createCountField(getTranslation("report.deceased"), AssignmentReport::getDeceased, AssignmentReport::setDeceased)
        );

        sceneBinder.addValueChangeListener(event -> markChanged());
        sceneLastEdit = createLastEditSpan();

        units = new VerticalLayout();
        units.setPadding(false);

        reportStatus = new Badge();
        closedBy = createLastEditSpan();
        var statusRow = new HorizontalLayout(reportStatus, closedBy);
        statusRow.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.BASELINE);

        save = new Button(getTranslation("report.save"), event -> save());
        save.addThemeVariants(ButtonVariant.PRIMARY);
        close = new Button(getTranslation("report.close"), VaadinIcon.LOCK.create(), event -> close());
        close.addThemeVariants(ButtonVariant.SUCCESS);
        reopen = new Button(getTranslation("report.reopen"), VaadinIcon.UNLOCK.create(), event -> reopen());
        var actions = new HorizontalLayout(save, close, reopen);

        var contentLayout = new VerticalLayout(statusRow, formLayout, resourcesGrid, new H3(getTranslation("report.on-scene")),
                sceneLastEdit, sceneForm, new H3(getTranslation("report.responding-units")), units, actions);
        content = new Scroller(contentLayout);
        content.setSizeFull();
        add(content);

        setBean(null);
    }

    private static Span createLastEditSpan() {
        var span = new Span();
        span.getStyle().set("color", "var(--vaadin-text-color-secondary)").set("font-size", "var(--aura-font-size-s)");
        return span;
    }

    private static String formatLastEdit(UserStamp lastEdit) {
        return lastEdit == null ? I18NProvider.translate("report.not-filled-in-yet")
                : I18NProvider.translate("report.last-edited", lastEdit.getDisplayName(),
                Formatters.formatDateTime(lastEdit.getTimestamp()));
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
                .withValidator(value -> value == null || value >= 0, getTranslation("report.cannot-be-negative"))
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
        closedBy.setText(reportClosed ? getTranslation("report.closed-by", bean.getReport().getClosed().getDisplayName(),
                Formatters.formatDateTime(bean.getReport().getClosed().getTimestamp())) : "");

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
                units.add(new Span(getTranslation("report.no-responding-units")));
            }
            respondingResources.forEach(resource -> {
                var unitReport = bean.getReport().getUnitReport(resource.id());
                var unitForm = new UnitReportForm(resource, unitReport, editable);
                unitForm.binder.addValueChangeListener(event -> markChanged());
                unitForms.add(unitForm);

                var details = new Details(createUnitSummary(resource, unitReport, unitForm.status()), unitForm);
                details.setWidthFull();
                // A writer usually works on their own unit only, so every unit starts closed and stays out of the way
                details.setOpened(openedUnits != null && openedUnits.contains(resource.id()));
                unitDetails.put(resource.id(), details);
                units.add(details);
            });
        }
    }

    private static Component createUnitSummary(ResourceListItem resource, UnitReport unitReport, Signal<UnitReportStatus> status) {
        var callSign = new Span(resource.callSign());
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
                unitDetails.get(unitForm.getResource().id()).setOpened(true);
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
                showNotification(getTranslation("report.save.closed-meanwhile"),
                        NotificationVariant.WARNING);
            } else if (result instanceof UpdateResult.Conflict<AssignmentReport> conflict && conflict.getConflictingData() != null) {
                showChangedReport(conflict.getConflictingData());
                showNotification(getTranslation("report.save.conflict"),
                        NotificationVariant.WARNING);
            } else {
                result.getData().ifPresent(this::showChangedReport);
                showNotification(getTranslation("report.saved"), NotificationVariant.SUCCESS);
            }
        } catch (ValidationFailedException ex) {
            showNotification(String.join("\n", ex.getViolationMessages(getLocale())), NotificationVariant.ERROR);
        }
    }

    private void close() {
        if (bean == null || !canReview) {
            return;
        }
        if (unsavedChanges) {
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
            showNotification(getTranslation("report.review.conflict"),
                    NotificationVariant.WARNING);
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
        NOT_FILLED_IN("report.unit.not-filled-in"),
        PARTLY_FILLED_IN("report.unit.partly-filled-in"),
        FILLED_IN("report.unit.filled-in");

        private final String labelKey;

        UnitReportStatus(String labelKey) {
            this.labelKey = labelKey;
        }

        String getLabel() {
            return I18NProvider.translate(labelKey);
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

        private final ResourceListItem resource;
        private final Binder<UnitReport> binder = new Binder<>();
        private final Signal<UnitReportStatus> status;

        UnitReportForm(ResourceListItem resource, UnitReport unitReport, boolean editable) {
            this.resource = resource;
            setPadding(false);
            setSpacing(false);

            var situationOnArrival = new TextArea(getTranslation("report.unit.situation-on-arrival"));
            var actionsTaken = new TextArea(getTranslation("report.unit.actions-taken"));
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

        ResourceListItem getResource() {
            return resource;
        }

        Signal<UnitReportStatus> status() {
            return status;
        }
    }
}
