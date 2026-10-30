package net.pkhapps.idispatch.server.ui.reports;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.masterdetaillayout.MasterDetailLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.function.SerializableRunnable;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.Lumo;
import jakarta.annotation.security.RolesAllowed;
import net.pkhapps.idispatch.server.boundary.AssignmentService;
import net.pkhapps.idispatch.server.boundary.ReportService;
import net.pkhapps.idispatch.server.entity.Assignment;
import net.pkhapps.idispatch.server.security.Roles;
import net.pkhapps.idispatch.server.ui.Formatters;
import net.pkhapps.idispatch.server.ui.HasNavbarContent;
import net.pkhapps.idispatch.server.ui.RootLayout;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@Route(value = "reports", layout = RootLayout.class)
@RolesAllowed({Roles.ROLE_REPORT_READER, Roles.ROLE_REPORT_WRITER})
@PageTitle("iDispatch | Reports")
@StyleSheet(Lumo.COMPACT_STYLESHEET)
public class ReportsView extends MasterDetailLayout implements HasNavbarContent, BeforeLeaveObserver {

    private final AssignmentService assignmentService;
    private final Button refreshButton;
    private final Grid<Assignment> assignmentsGrid;

    private final AssignmentReportPanel reportPanel;
    private boolean revertingSelection;
    private ConfirmDialog discardDialog;
    private SerializableRunnable actionAfterDiscard;

    public ReportsView(AssignmentService assignmentService, ReportService reportService) {
        this.assignmentService = assignmentService;

        assignmentsGrid = new Grid<>();
        assignmentsGrid.addColumn(Assignment::getId).setHeader("Number")
                .setSortable(true).setTextAlign(ColumnTextAlign.END).setAutoWidth(true).setFlexGrow(0);
        var closedColumn = assignmentsGrid.addColumn(assignment -> Formatters.formatDateTime(assignment.getClosed()))
                .setHeader("Closed").setComparator(Assignment::getClosed).setAutoWidth(true).setFlexGrow(0);
        assignmentsGrid.addColumn(Assignment::getUrgency).setHeader("Urgency")
                .setComparator(Assignment::getUrgency).setAutoWidth(true).setFlexGrow(0);
        assignmentsGrid.addColumn(assignment -> Formatters.formatAssignmentType(assignment.getType())).setHeader("Type")
                .setSortable(true).setResizable(true);
        assignmentsGrid.addColumn(assignment -> Formatters.formatMunicipality(assignment.getMunicipality()))
                .setHeader("Municipality").setSortable(true).setResizable(true);
        assignmentsGrid.addColumn(Assignment::getAddress).setHeader("Address")
                .setSortable(true).setResizable(true);
        assignmentsGrid.sort(GridSortOrder.desc(closedColumn).build());
        assignmentsGrid.setSizeFull();
        assignmentsGrid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_COMPACT, GridVariant.LUMO_ROW_STRIPES);

        var authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        reportPanel = new AssignmentReportPanel(reportService, authorities.stream()
                .anyMatch(authority -> Roles.ROLE_REPORT_WRITER.equals(authority.getAuthority())));
        reportPanel.setSizeFull();

        refreshButton = new Button(VaadinIcon.REFRESH.create(), event -> discardChangesThen(this::refresh));

        assignmentsGrid.asSingleSelect().addValueChangeListener(event -> {
            if (revertingSelection) {
                return;
            }
            if (reportPanel.hasUnsavedChanges()) {
                // Keep the report with the unsaved changes selected until the user decides to throw them away
                revertingSelection = true;
                try {
                    assignmentsGrid.asSingleSelect().setValue(event.getOldValue());
                } finally {
                    revertingSelection = false;
                }
                discardChangesThen(() -> assignmentsGrid.asSingleSelect().setValue(event.getValue()));
                return;
            }
            var report = Optional.ofNullable(event.getValue())
                    .flatMap(assignment -> reportService.findReportByAssignmentId(assignment.nullSafeId()))
                    .orElse(null);
            reportPanel.setBean(report);
            setDetail(report == null ? null : reportPanel);
        });

        setSizeFull();
        setMasterSize("30em");
        setDetailSize("40em");
        setExpandMaster(true);
        setMaster(assignmentsGrid);
        setDetailPlaceholder(new Span("No assignment selected"));

        // When the report is shown on top of the list, closing it deselects the assignment, which asks first if there
        // are unsaved changes
        addBackdropClickListener(event -> assignmentsGrid.asSingleSelect().clear());
        addDetailEscapePressListener(event -> assignmentsGrid.asSingleSelect().clear());
    }

    /**
     * Runs the action right away if the report has no unsaved changes. Otherwise, asks the user whether to throw the
     * changes away, and runs the action only if they do. While the question is open, a newer action replaces the
     * older one, so that the user is asked only once.
     */
    private void discardChangesThen(SerializableRunnable action) {
        if (!reportPanel.hasUnsavedChanges()) {
            action.run();
            return;
        }
        actionAfterDiscard = action;
        if (discardDialog != null) {
            return;
        }
        discardDialog = new ConfirmDialog();
        discardDialog.setHeader("Unsaved changes");
        discardDialog.setText("You have not saved this report. Do you want to throw away your changes?");
        discardDialog.setCancelable(true);
        discardDialog.setCancelText("Keep Editing");
        discardDialog.setConfirmText("Throw Away");
        discardDialog.setConfirmButtonTheme("error primary");
        discardDialog.addConfirmListener(event -> {
            var actionToRun = actionAfterDiscard;
            closeDiscardDialog();
            reportPanel.setBean(null);
            actionToRun.run();
        });
        discardDialog.addCancelListener(event -> closeDiscardDialog());
        // However the dialog closes, even with Esc, the next unsaved change must be able to open a new one
        discardDialog.addOpenedChangeListener(event -> {
            if (!event.isOpened() && event.getSource() == discardDialog) {
                discardDialog = null;
                actionAfterDiscard = null;
            }
        });
        discardDialog.open();
    }

    private void closeDiscardDialog() {
        if (discardDialog != null) {
            discardDialog.close();
        }
    }

    @Override
    public void beforeLeave(BeforeLeaveEvent event) {
        if (reportPanel.hasUnsavedChanges()) {
            var postponed = event.postpone();
            discardChangesThen(postponed::proceed);
        }
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
    public Optional<Component> getNavbarContent() {
        var toolbar = new HorizontalLayout(refreshButton);
        toolbar.getStyle().set("margin-left", "var(--lumo-space-m)");
        toolbar.setSizeUndefined();
        return Optional.of(toolbar);
    }

    @Override
    public String getTitle() {
        return "Reports";
    }
}
