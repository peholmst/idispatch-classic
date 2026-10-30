package net.pkhapps.idispatch.server.ui.reports;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.masterdetaillayout.MasterDetailLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
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
import net.pkhapps.idispatch.server.entity.AssignmentType;
import net.pkhapps.idispatch.server.entity.Municipality;
import net.pkhapps.idispatch.server.security.Roles;
import net.pkhapps.idispatch.server.ui.Formatters;
import net.pkhapps.idispatch.server.ui.HasNavbarContent;
import net.pkhapps.idispatch.server.ui.RootLayout;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

@Route(value = "reports", layout = RootLayout.class)
@RolesAllowed({Roles.ROLE_REPORT_READER, Roles.ROLE_REPORT_WRITER, Roles.ROLE_REPORT_REVIEWER})
@PageTitle("iDispatch | Reports")
@StyleSheet(Lumo.COMPACT_STYLESHEET)
public class ReportsView extends MasterDetailLayout implements HasNavbarContent, BeforeLeaveObserver {

    private final AssignmentService assignmentService;
    private final ReportService reportService;
    private final Button refreshButton;
    private final Grid<Assignment> assignmentsGrid;
    private final DatePicker closedFrom;
    private final DatePicker closedTo;
    private final ComboBox<Municipality> municipalityFilter;
    private final ComboBox<AssignmentType> typeFilter;
    private final ComboBox<ReportStatus> reportStatusFilter;
    private Set<Long> closedReports = new HashSet<>();
    private GridListDataView<Assignment> assignmentsDataView;

    private final AssignmentReportPanel reportPanel;
    private boolean revertingSelection;
    private ConfirmDialog discardDialog;
    private SerializableRunnable actionAfterDiscard;

    public ReportsView(AssignmentService assignmentService, ReportService reportService) {
        this.assignmentService = assignmentService;
        this.reportService = reportService;

        assignmentsGrid = new Grid<>();
        assignmentsGrid.addColumn(Assignment::getId).setHeader("Number")
                .setSortable(true).setTextAlign(ColumnTextAlign.END).setAutoWidth(true).setFlexGrow(0);
        assignmentsGrid.addComponentColumn(assignment -> reportStatus(assignment).createBadge()).setHeader("Report")
                .setComparator(this::reportStatus).setAutoWidth(true).setFlexGrow(0);
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

        var authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        reportPanel = new AssignmentReportPanel(reportService, authorities.contains(Roles.ROLE_REPORT_WRITER),
                authorities.contains(Roles.ROLE_REPORT_REVIEWER));
        reportPanel.setSizeFull();
        // Keeps the Report column and the status filter up to date when the user closes or re-opens a report
        reportPanel.setReportChangeListener(report -> {
            if (report.isClosed()) {
                closedReports.add(report.getAssignment().getId());
            } else {
                closedReports.remove(report.getAssignment().getId());
            }
            assignmentsDataView.refreshAll();
        });

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

        closedFrom = createDatePicker("Closed from");
        closedTo = createDatePicker("Closed to");
        closedFrom.addValueChangeListener(event -> closedTo.setMin(event.getValue()));
        closedTo.addValueChangeListener(event -> closedFrom.setMax(event.getValue()));

        municipalityFilter = new ComboBox<>("Municipality");
        municipalityFilter.setItemLabelGenerator(Formatters::formatMunicipality);
        typeFilter = new ComboBox<>("Type");
        typeFilter.setItemLabelGenerator(Formatters::formatAssignmentType);
        for (var comboBox : List.of(municipalityFilter, typeFilter)) {
            comboBox.setClearButtonVisible(true);
            comboBox.setWidth("12em");
        }
        reportStatusFilter = new ComboBox<>("Report");
        reportStatusFilter.setItems(ReportStatus.values());
        reportStatusFilter.setItemLabelGenerator(ReportStatus::getLabel);
        reportStatusFilter.setClearButtonVisible(true);
        reportStatusFilter.setWidth("9em");

        for (var filter : List.<HasValue<?, ?>>of(closedFrom, closedTo, municipalityFilter, typeFilter, reportStatusFilter)) {
            filter.addValueChangeListener(event -> {
                if (assignmentsDataView != null) {
                    assignmentsDataView.refreshAll();
                }
            });
        }

        var clearFilters = new Button("Clear", VaadinIcon.CLOSE_SMALL.create(), event -> {
            closedFrom.clear();
            closedTo.clear();
            municipalityFilter.clear();
            typeFilter.clear();
            reportStatusFilter.clear();
        });
        clearFilters.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        var filterBar = new HorizontalLayout(closedFrom, closedTo, municipalityFilter, typeFilter, reportStatusFilter,
                clearFilters);
        filterBar.setWrap(true);
        filterBar.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);
        filterBar.getStyle().set("padding", "0 var(--lumo-space-m) var(--lumo-space-s)");

        var master = new VerticalLayout(filterBar, assignmentsGrid);
        master.setSizeFull();
        master.setPadding(false);
        master.setSpacing(false);
        master.setFlexGrow(1, assignmentsGrid);

        setSizeFull();
        setMasterSize("30em");
        setDetailSize("40em");
        setExpandMaster(true);
        setMaster(master);
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

    private static DatePicker createDatePicker(String label) {
        var datePicker = new DatePicker(label);
        var i18n = new DatePicker.DatePickerI18n();
        i18n.setDateFormat("dd.MM.yyyy");
        i18n.setFirstDayOfWeek(1);
        datePicker.setI18n(i18n);
        datePicker.setClearButtonVisible(true);
        datePicker.setWidth("10em");
        return datePicker;
    }

    private void refresh() {
        assignmentsGrid.deselectAll();
        var assignments = assignmentService.findClosedAssignments();
        closedReports = new HashSet<>(reportService.findAssignmentIdsWithClosedReports());
        assignmentsDataView = assignmentsGrid.setItems(assignments);
        assignmentsDataView.setFilter(this::matchesFilters);
        // Offer only the municipalities and types that some closed assignment has, so every choice matches something
        updateChoices(municipalityFilter, assignments.stream().map(Assignment::getMunicipality),
                Comparator.comparing(Municipality::getName));
        updateChoices(typeFilter, assignments.stream().map(Assignment::getType),
                Comparator.comparing(AssignmentType::getFormattedDescription));
    }

    private static <T> void updateChoices(ComboBox<T> comboBox, Stream<T> values, Comparator<T> order) {
        var selected = comboBox.getValue();
        var choices = values.filter(Objects::nonNull).distinct().sorted(order).toList();
        comboBox.setItems(choices);
        if (selected != null && choices.contains(selected)) {
            comboBox.setValue(selected);
        }
    }

    private boolean matchesFilters(Assignment assignment) {
        var closedDate = LocalDate.ofInstant(assignment.getClosed(), ZoneId.systemDefault());
        return (closedFrom.getValue() == null || !closedDate.isBefore(closedFrom.getValue()))
                && (closedTo.getValue() == null || !closedDate.isAfter(closedTo.getValue()))
                && (municipalityFilter.getValue() == null || municipalityFilter.getValue().equals(assignment.getMunicipality()))
                && (typeFilter.getValue() == null || typeFilter.getValue().equals(assignment.getType()))
                && (reportStatusFilter.getValue() == null || reportStatusFilter.getValue() == reportStatus(assignment));
    }

    private ReportStatus reportStatus(Assignment assignment) {
        return ReportStatus.of(closedReports.contains(assignment.getId()));
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
