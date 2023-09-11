package net.pkhapps.idispatch.server.ui.reports;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.dom.ElementFactory;
import net.pkhapps.idispatch.server.boundary.ReportDTO;
import net.pkhapps.idispatch.server.boundary.ReportResourceDTO;
import net.pkhapps.idispatch.server.entity.AssignmentUrgency;
import net.pkhapps.idispatch.server.ui.dws.Formatters;

import java.util.Collections;
import java.util.Objects;

class AssignmentReportPanel extends VerticalLayout {

    private final Binder<ReportDTO> binder;
    private final Grid<ReportResourceDTO> resourcesGrid;
    private final Component noAssignmentSelected;
    private final FormLayout formLayout;

    AssignmentReportPanel() {
        setSizeFull();
        getStyle().set("background-color", "var(--lumo-shade-10pct)");

        noAssignmentSelected = new VerticalLayout(new Span("No assignment selected"));
        add(noAssignmentSelected);

        binder = new Binder<>();
        formLayout = new FormLayout();
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

        add(formLayout);

        resourcesGrid = new Grid<>();
        resourcesGrid.setSizeFull();
        resourcesGrid.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_ROW_STRIPES);
        resourcesGrid.setSelectionMode(Grid.SelectionMode.NONE);
        addAndExpand(resourcesGrid);

        resourcesGrid.addColumn(ReportResourceDTO::getCallSign).setHeader("Resource");
        resourcesGrid.addColumn(dto -> Formatters.formatDateTime(dto.getDispatched())).setHeader("Dispatched");
        resourcesGrid.addColumn(dto -> Formatters.formatDateTime(dto.getEnRoute())).setHeader("En route");
        resourcesGrid.addColumn(dto -> Formatters.formatDateTime(dto.getOnScene())).setHeader("On scene");
        resourcesGrid.addColumn(dto -> Formatters.formatDateTime(dto.getAvailable())).setHeader("Available");
        resourcesGrid.addColumn(dto -> Formatters.formatDateTime(dto.getAtStation())).setHeader("At station");

        setBean(null);
    }

    public void setBean(ReportDTO bean) {
        noAssignmentSelected.setVisible(bean == null);
        formLayout.setVisible(bean != null);
        resourcesGrid.setVisible(bean != null);

        binder.setBean(bean);
        resourcesGrid.setItems(bean == null ? Collections.emptyList() : bean.getResources());
    }

    private <M, V> M noConverter(V presentation) {
        throw new UnsupportedOperationException("Conversion to model is not supported");
    }
}
