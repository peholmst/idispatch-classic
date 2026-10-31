package net.pkhapps.idispatch.server.report;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.dataview.GridLazyDataView;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.masterdetaillayout.MasterDetailLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.function.SerializableRunnable;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import net.pkhapps.idispatch.server.assignmenttype.AssignmentTypeFormatters;
import net.pkhapps.idispatch.server.assignmenttype.AssignmentTypeListItem;
import net.pkhapps.idispatch.server.municipality.MunicipalityFormatters;
import net.pkhapps.idispatch.server.municipality.MunicipalityListItem;
import net.pkhapps.idispatch.server.security.Roles;
import net.pkhapps.idispatch.server.shared.Bilingual;
import net.pkhapps.idispatch.server.ui.Formatters;
import net.pkhapps.idispatch.server.ui.RootLayout;
import net.pkhapps.idispatch.server.ui.ViewTitle;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.DayOfWeek;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

@Route(value = "reports", layout = RootLayout.class)
@RolesAllowed({Roles.ROLE_REPORT_READER, Roles.ROLE_REPORT_WRITER, Roles.ROLE_REPORT_REVIEWER})
public class ReportsView extends MasterDetailLayout implements HasDynamicTitle, BeforeLeaveObserver {

    private final ReportService reportService;
    private final Grid<ClosedAssignmentListItem> assignmentsGrid;
    private final DatePicker closedFrom;
    private final DatePicker closedTo;
    private final ComboBox<MunicipalityListItem> municipalityFilter;
    private final ComboBox<AssignmentTypeListItem> typeFilter;
    private final ComboBox<ReportStatus> reportStatusFilter;
    private final GridLazyDataView<ClosedAssignmentListItem> assignmentsDataView;

    private final AssignmentReportPanel reportPanel;
    private boolean revertingSelection;
    private ConfirmDialog discardDialog;
    private SerializableRunnable actionAfterDiscard;

    public ReportsView(ReportService reportService) {
        this.reportService = reportService;

        // The database sorts the assignments by the sort properties
        assignmentsGrid = new Grid<>();
        assignmentsGrid.addColumn(item -> item.id().value()).setHeader(getTranslation("report.number"))
                .setSortProperty(ClosedAssignmentListItem.SORT_NUMBER).setTextAlign(ColumnTextAlign.END).setAutoWidth(true).setFlexGrow(0);
        assignmentsGrid.addComponentColumn(item -> ReportStatus.of(item.reportClosed()).createBadge())
                .setHeader(getTranslation("report.report")).setSortProperty(ClosedAssignmentListItem.SORT_REPORT_CLOSED)
                .setAutoWidth(true).setFlexGrow(0);
        var closedColumn = assignmentsGrid.addColumn(item -> Formatters.formatDateTime(item.closed()))
                .setHeader(getTranslation("report.closed")).setSortProperty(ClosedAssignmentListItem.SORT_CLOSED).setAutoWidth(true)
                .setFlexGrow(0);
        assignmentsGrid.addColumn(ClosedAssignmentListItem::urgency).setHeader(getTranslation("assignment.urgency"))
                .setSortProperty(ClosedAssignmentListItem.SORT_URGENCY).setAutoWidth(true).setFlexGrow(0);
        assignmentsGrid.addColumn(item -> item.typeCode() == null ? null
                        : AssignmentTypeFormatters.formatAssignmentType(item.typeCode(),
                        Bilingual.select(item.typeDescriptionFi(), item.typeDescriptionSv(), getLocale())))
                .setHeader(getTranslation("assignment.type"))
                .setSortProperty(Stream.concat(Stream.of(ClosedAssignmentListItem.SORT_TYPE_CODE),
                                inPreferredOrder(ClosedAssignmentListItem.SORT_TYPE_DESCRIPTION_FI,
                                        ClosedAssignmentListItem.SORT_TYPE_DESCRIPTION_SV))
                        .toArray(String[]::new))
                .setResizable(true);
        assignmentsGrid.addColumn(item -> item.municipalityNameFi() == null && item.municipalityNameSv() == null ? null
                        : Bilingual.select(item.municipalityNameFi(), item.municipalityNameSv(), getLocale()))
                .setHeader(getTranslation("assignment.municipality"))
                .setSortProperty(inPreferredOrder(ClosedAssignmentListItem.SORT_MUNICIPALITY_NAME_FI,
                        ClosedAssignmentListItem.SORT_MUNICIPALITY_NAME_SV).toArray(String[]::new))
                .setResizable(true);
        assignmentsGrid.addColumn(ClosedAssignmentListItem::address).setHeader(getTranslation("assignment.address"))
                .setSortProperty(ClosedAssignmentListItem.SORT_ADDRESS).setResizable(true);
        assignmentsGrid.sort(GridSortOrder.desc(closedColumn).build());
        assignmentsDataView = assignmentsGrid.setItemsPageable(
                pageable -> reportService.findClosedAssignments(currentFilter(), pageable),
                pageable -> reportService.countClosedAssignments(currentFilter()));
        // The report status changes when a reviewer closes or re-opens the report, but it is still the same row
        assignmentsDataView.setIdentifierProvider(ClosedAssignmentListItem::id);
        assignmentsGrid.setEmptyStateText(getTranslation("report.empty"));
        assignmentsGrid.setSizeFull();
        assignmentsGrid.addThemeVariants(GridVariant.NO_BORDER, GridVariant.ROW_STRIPES);

        var authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        reportPanel = new AssignmentReportPanel(reportService, authorities.contains(Roles.ROLE_REPORT_WRITER),
                authorities.contains(Roles.ROLE_REPORT_REVIEWER));
        reportPanel.setSizeFull();
        // Keeps the Report column and the status filter up to date when the user closes or re-opens a report
        reportPanel.setReportChangeListener(report -> assignmentsDataView.refreshAll());

        var refreshButton = new Button(getTranslation("report.refresh"), VaadinIcon.REFRESH.create(),
                event -> discardChangesThen(this::refresh));
        refreshButton.addThemeVariants(ButtonVariant.TERTIARY);

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
                    .flatMap(item -> reportService.findReportByAssignmentId(item.id().value()))
                    .orElse(null);
            reportPanel.setBean(report);
            setDetail(report == null ? null : reportPanel);
        });

        closedFrom = createDatePicker(getTranslation("report.filter.closed-from"));
        closedTo = createDatePicker(getTranslation("report.filter.closed-to"));
        closedFrom.addValueChangeListener(event -> closedTo.setMin(event.getValue()));
        closedTo.addValueChangeListener(event -> closedFrom.setMax(event.getValue()));

        municipalityFilter = new ComboBox<>(getTranslation("assignment.municipality"));
        municipalityFilter.setItemLabelGenerator(MunicipalityFormatters::formatMunicipality);
        typeFilter = new ComboBox<>(getTranslation("assignment.type"));
        typeFilter.setItemLabelGenerator(AssignmentTypeFormatters::formatAssignmentType);
        for (var comboBox : List.of(municipalityFilter, typeFilter)) {
            comboBox.setClearButtonVisible(true);
            comboBox.setWidth("12em");
        }
        reportStatusFilter = new ComboBox<>(getTranslation("report.report"));
        reportStatusFilter.setItems(ReportStatus.values());
        reportStatusFilter.setItemLabelGenerator(ReportStatus::getLabel);
        reportStatusFilter.setClearButtonVisible(true);
        reportStatusFilter.setWidth("9em");

        for (var filter : List.<HasValue<?, ?>>of(closedFrom, closedTo, municipalityFilter, typeFilter, reportStatusFilter)) {
            filter.addValueChangeListener(event -> assignmentsDataView.refreshAll());
        }

        var clearFilters = new Button(getTranslation("report.filter.clear"), VaadinIcon.CLOSE_SMALL.create(), event -> {
            closedFrom.clear();
            closedTo.clear();
            municipalityFilter.clear();
            typeFilter.clear();
            reportStatusFilter.clear();
        });
        clearFilters.addThemeVariants(ButtonVariant.TERTIARY);

        var filterBar = new HorizontalLayout(closedFrom, closedTo, municipalityFilter, typeFilter, reportStatusFilter,
                clearFilters, refreshButton);
        filterBar.setWrap(true);
        filterBar.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);
        filterBar.addClassName("toolbar");
        filterBar.setWidthFull();
        filterBar.setPadding(true);

        var toolbar = new HorizontalLayout(new ViewTitle(getTranslation("nav.reports")));
        toolbar.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        toolbar.setWidthFull();
        toolbar.setPadding(true);
        toolbar.addClassName("toolbar");

        var master = new VerticalLayout(toolbar, filterBar, assignmentsGrid);
        master.setSizeFull();
        master.setPadding(false);
        master.setSpacing(false);
        master.setFlexGrow(1, assignmentsGrid);

        setSizeFull();
        setMasterSize("30em");
        setDetailSize("40em");
        setExpandMaster(true);
        setMaster(master);
        setDetailPlaceholder(new VerticalLayout(new Span(getTranslation("assignment.none-selected"))));

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
        discardDialog.setHeader(getTranslation("report.discard.title"));
        discardDialog.setText(getTranslation("report.discard.text"));
        discardDialog.setCancelable(true);
        discardDialog.setCancelText(getTranslation("report.discard.keep-editing"));
        discardDialog.setConfirmText(getTranslation("report.discard.throw-away"));
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

    private DatePicker createDatePicker(String label) {
        var datePicker = new DatePicker(label);
        var locale = getLocale();
        var i18n = new DatePicker.DatePickerI18n();
        i18n.setDateFormat("dd.MM.yyyy");
        i18n.setFirstDayOfWeek(1);
        i18n.setMonthNames(Stream.of(Month.values())
                .map(month -> capitalize(month.getDisplayName(TextStyle.FULL_STANDALONE, locale), locale))
                .toList());
        // The date picker starts the week on Sunday
        var weekdays = Stream.concat(Stream.of(DayOfWeek.SUNDAY), Stream.of(DayOfWeek.values()).limit(6)).toList();
        i18n.setWeekdays(weekdays.stream()
                .map(day -> capitalize(day.getDisplayName(TextStyle.FULL_STANDALONE, locale), locale))
                .toList());
        i18n.setWeekdaysShort(weekdays.stream()
                .map(day -> capitalize(day.getDisplayName(TextStyle.SHORT_STANDALONE, locale), locale))
                .toList());
        i18n.setToday(getTranslation("date-picker.today"));
        i18n.setCancel(getTranslation("common.cancel"));
        datePicker.setI18n(i18n);
        datePicker.setClearButtonVisible(true);
        datePicker.setWidth("10em");
        return datePicker;
    }

    private static String capitalize(String text, Locale locale) {
        return text.isEmpty() ? text : text.substring(0, 1).toUpperCase(locale) + text.substring(1);
    }

    private void refresh() {
        assignmentsGrid.deselectAll();
        // Offer only the municipalities and types that some closed assignment has, so every choice matches something
        updateChoices(municipalityFilter, Formatters.sortedByLabel(reportService.findMunicipalitiesOfClosedAssignments(),
                MunicipalityFormatters::formatMunicipality));
        updateChoices(typeFilter, Formatters.sortedByLabel(reportService.findTypesOfClosedAssignments(),
                AssignmentTypeFormatters::formatAssignmentType));
        assignmentsDataView.refreshAll();
    }

    /**
     * Returns the Finnish and Swedish properties of a bilingual text, the one shown in the user's language first. The
     * database sorts by both, so a row that only has the other language is sorted by that one among the rows that
     * have neither.
     */
    private Stream<String> inPreferredOrder(String finnishProperty, String swedishProperty) {
        return Bilingual.isSwedish(getLocale())
                ? Stream.of(swedishProperty, finnishProperty)
                : Stream.of(finnishProperty, swedishProperty);
    }

    private static <T> void updateChoices(ComboBox<T> comboBox, List<T> choices) {
        var selected = comboBox.getValue();
        comboBox.setItems(choices);
        if (selected != null && choices.contains(selected)) {
            comboBox.setValue(selected);
        }
    }

    private ClosedAssignmentFilter currentFilter() {
        var reportStatus = reportStatusFilter.getValue();
        var municipality = municipalityFilter.getValue();
        var type = typeFilter.getValue();
        return new ClosedAssignmentFilter(closedFrom.getValue(), closedTo.getValue(),
                municipality == null ? null : municipality.id(), type == null ? null : type.id(),
                reportStatus == null ? null : reportStatus == ReportStatus.CLOSED);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        refresh();
    }

    @Override
    public String getPageTitle() {
        return "iDispatch | " + getTranslation("nav.reports");
    }
}
