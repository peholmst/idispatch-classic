package net.pkhapps.idispatch.server.ui.reports;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.splitlayout.SplitLayoutVariant;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import net.pkhapps.idispatch.server.boundary.AssignmentService;
import net.pkhapps.idispatch.server.boundary.ReportService;
import net.pkhapps.idispatch.server.entity.Assignment;
import net.pkhapps.idispatch.server.security.Roles;
import net.pkhapps.idispatch.server.ui.HasNavbarContent;
import net.pkhapps.idispatch.server.ui.RootLayout;
import net.pkhapps.idispatch.server.ui.dws.Formatters;

@Route(value = "reports", layout = RootLayout.class)
@RolesAllowed(Roles.ROLE_REPORT_READER)
@PageTitle("iDispatch | Reports")
@JsModule("@vaadin/vaadin-lumo-styles/presets/compact.js")
public class ReportsView extends SplitLayout implements HasNavbarContent {

    private final AssignmentService assignmentService;
    private final Button refreshButton;
    private final Grid<Assignment> assignmentsGrid;

    private final AssignmentReportPanel reportPanel;

    public ReportsView(AssignmentService assignmentService, ReportService reportService) {
        this.assignmentService = assignmentService;

        assignmentsGrid = new Grid<>();
        assignmentsGrid.addColumn(Formatters::formatAssignment);
        assignmentsGrid.setSizeFull();
        assignmentsGrid.addThemeVariants(
                GridVariant.LUMO_NO_BORDER,
                GridVariant.LUMO_COMPACT
        );
        assignmentsGrid.getStyle().set("background", "none");

        reportPanel = new AssignmentReportPanel();
        reportPanel.setSizeFull();

        refreshButton = new Button(VaadinIcon.REFRESH.create(), event -> refresh());

        assignmentsGrid.addSelectionListener(event -> reportPanel.setBean(event.getFirstSelectedItem()
                .flatMap(assignment -> reportService.findReportByAssignmentId(assignment.nullSafeId()))
                .orElse(null)));

        setOrientation(Orientation.HORIZONTAL);
        addThemeVariants(SplitLayoutVariant.LUMO_SMALL);
        setSplitterPosition(25);
        setSizeFull();

        addToPrimary(assignmentsGrid);
        addToSecondary(reportPanel);
    }

    private void refresh() {
        assignmentsGrid.deselectAll();
        assignmentsGrid.setItems(assignmentService.findClosedAssignments());
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        refresh();
    }

    @Override
    public Component getNavbarContent() {
        var toolbar = new HorizontalLayout(refreshButton);
        toolbar.getStyle().set("margin-left", "var(--lumo-space-m)");
        toolbar.setSizeUndefined();
        return toolbar;
    }

    @Override
    public String getTitle() {
        return "Reports";
    }
}
