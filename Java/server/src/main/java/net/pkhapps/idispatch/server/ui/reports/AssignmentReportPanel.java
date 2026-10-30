package net.pkhapps.idispatch.server.ui.reports;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.data.binder.Setter;
import net.pkhapps.idispatch.server.boundary.ReportDTO;
import net.pkhapps.idispatch.server.boundary.ReportResourceDTO;
import net.pkhapps.idispatch.server.boundary.ReportService;
import net.pkhapps.idispatch.server.entity.AssignmentReport;
import net.pkhapps.idispatch.server.entity.AssignmentUrgency;
import net.pkhapps.idispatch.server.entity.LastEdit;
import net.pkhapps.idispatch.server.entity.Resource;
import net.pkhapps.idispatch.server.entity.UnitReport;
import net.pkhapps.idispatch.server.entity.ValidationFailedException;
import net.pkhapps.idispatch.server.ui.Formatters;
import net.pkhapps.idispatch.server.util.UpdateResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

class AssignmentReportPanel extends VerticalLayout {

    private final ReportService reportService;
    private final boolean editable;
    private final Binder<ReportDTO> binder;
    private final Binder<AssignmentReport> sceneBinder;
    private final List<UnitReportForm> unitForms = new ArrayList<>();
    private final Grid<ReportResourceDTO> resourcesGrid;
    private final Component noAssignmentSelected;
    private final Scroller content;
    private final VerticalLayout units;
    private final Span sceneLastEdit;
    private ReportDTO bean;
    private boolean loading;
    private boolean unsavedChanges;

    /**
     * @param editable whether the user may fill in what happened on scene, or only read it
     */
    AssignmentReportPanel(ReportService reportService, boolean editable) {
        this.reportService = reportService;
        this.editable = editable;
        setSizeFull();
        setPadding(false);
        getStyle().set("background-color", "var(--lumo-shade-10pct)");

        noAssignmentSelected = new VerticalLayout(new Span("No assignment selected"));
        add(noAssignmentSelected);

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
        resourcesGrid.addColumn(dto -> Formatters.formatDateTime(dto.getDispatched())).setHeader("Dispatched");
        resourcesGrid.addColumn(dto -> Formatters.formatDateTime(dto.getEnRoute())).setHeader("En route");
        resourcesGrid.addColumn(dto -> Formatters.formatDateTime(dto.getOnScene())).setHeader("On scene");
        resourcesGrid.addColumn(dto -> Formatters.formatDateTime(dto.getAvailable())).setHeader("Available");
        resourcesGrid.addColumn(dto -> Formatters.formatDateTime(dto.getAtStation())).setHeader("At station");

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

        var save = new Button("Save Report", event -> save());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.setVisible(editable);
        sceneBinder.setReadOnly(!editable);

        var contentLayout = new VerticalLayout(formLayout, resourcesGrid, new H3("What happened on scene"), sceneLastEdit, sceneForm,
                new H3("Responding units"), units, save);
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

    private static String formatLastEdit(LastEdit lastEdit) {
        return lastEdit == null ? "Not filled in yet"
                : "Last edited by " + lastEdit.getDisplayName() + " on " + Formatters.formatDateTime(lastEdit.getEditedAt());
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
        this.bean = bean;
        noAssignmentSelected.setVisible(bean == null);
        content.setVisible(bean != null);

        binder.setBean(bean);
        resourcesGrid.setItems(bean == null ? Collections.emptyList() : bean.getResources());
        sceneBinder.setBean(bean == null ? null : bean.getReport());
        sceneLastEdit.setText(bean == null ? "" : formatLastEdit(bean.getReport().getLastEdit()));

        units.removeAll();
        unitForms.clear();
        if (bean != null) {
            var respondingResources = bean.getRespondingResources();
            if (respondingResources.isEmpty()) {
                units.add(new Span("No units responded to this assignment"));
            }
            respondingResources.forEach(resource -> {
                var unitForm = new UnitReportForm(resource, bean.getReport().getUnitReport(resource), editable);
                unitForm.binder.addValueChangeListener(event -> markChanged());
                unitForms.add(unitForm);
                units.add(unitForm);
            });
        }
    }

    private void save() {
        if (bean == null || !editable) {
            return;
        }
        var valid = sceneBinder.validate().isOk();
        for (var unitForm : unitForms) {
            valid &= unitForm.binder.validate().isOk();
        }
        if (!valid) {
            return;
        }
        try {
            var result = reportService.saveReport(bean.getReport());
            if (result instanceof UpdateResult.Conflict<AssignmentReport> conflict && conflict.getConflictingData() != null) {
                setBean(bean.withReport(conflict.getConflictingData()));
                showNotification("Someone else saved this report while you were writing yours. Their version is shown now, so add your changes again.",
                        NotificationVariant.LUMO_WARNING);
            } else {
                result.getData().ifPresent(report -> setBean(bean.withReport(report)));
                showNotification("Report saved", NotificationVariant.LUMO_SUCCESS);
            }
        } catch (ValidationFailedException ex) {
            showNotification(String.join("\n", ex.getViolationMessages(Locale.getDefault())), NotificationVariant.LUMO_ERROR);
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
     * What one responding unit found and did on scene.
     */
    static class UnitReportForm extends VerticalLayout {

        private final Resource resource;
        private final Binder<UnitReport> binder = new Binder<>();

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
            }
            binder.forField(situationOnArrival).bind(UnitReport::getSituationOnArrival, UnitReport::setSituationOnArrival);
            binder.forField(actionsTaken).bind(UnitReport::getActionsTaken, UnitReport::setActionsTaken);
            binder.setBean(unitReport);
            binder.setReadOnly(!editable);

            var lastEdit = createLastEditSpan();
            lastEdit.setText(formatLastEdit(unitReport.getLastEdit()));
            add(new H4(resource.getCallSign()), lastEdit, situationOnArrival, actionsTaken);
        }

        Resource getResource() {
            return resource;
        }
    }
}
