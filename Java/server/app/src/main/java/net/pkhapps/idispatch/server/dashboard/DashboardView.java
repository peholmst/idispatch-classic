package net.pkhapps.idispatch.server.dashboard;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import jakarta.annotation.security.PermitAll;
import net.pkhapps.idispatch.server.ui.RootLayout;
import net.pkhapps.idispatch.server.ui.ViewTitle;
import org.jspecify.annotations.Nullable;

/**
 * The home view: how many assignments are active, how many reports wait for a reviewer, and how many assignments each
 * unit has been on during the past 24 hours, the most used units first. It reloads the figures whenever the user
 * navigates to it, and every half minute while it is open.
 */
@Route(value = "", layout = RootLayout.class)
@PermitAll
public class DashboardView extends VerticalLayout implements HasDynamicTitle, AfterNavigationObserver {

    private static final int REFRESH_INTERVAL_MS = 30_000;

    private final DashboardService dashboardService;
    private final Span activeAssignments = new Span();
    private final Span openReports = new Span();
    private final Grid<UnitUtilization> unitGrid = new Grid<>();
    private @Nullable Registration pollRegistration;

    public DashboardView(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
        addClassName("dashboard-view");
        setSizeFull();

        var toolbar = new HorizontalLayout(new ViewTitle(getTranslation("nav.home")));
        toolbar.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        toolbar.addClassName("toolbar");
        toolbar.setWidthFull();
        toolbar.setPadding(true);

        var figures = new HorizontalLayout(
                createFigure(getTranslation("dashboard.active-assignments"), activeAssignments),
                createFigure(getTranslation("dashboard.open-reports"), openReports));
        figures.setWrap(true);
        figures.addClassName("dashboard-figures");

        unitGrid.addColumn(UnitUtilization::callSign).setHeader(getTranslation("management.call-sign"))
                .setAutoWidth(true).setFlexGrow(0);
        unitGrid.addColumn(UnitUtilization::assignments).setHeader(getTranslation("dashboard.assignments-24h"))
                .setTextAlign(ColumnTextAlign.END).setAutoWidth(true);
        unitGrid.setEmptyStateText(getTranslation("dashboard.no-units"));
        unitGrid.addThemeVariants(GridVariant.ROW_STRIPES);
        unitGrid.setMaxWidth("40rem");

        var unitsHeading = new H3(getTranslation("dashboard.units"));
        unitsHeading.addClassName("dashboard-heading");
        var units = new VerticalLayout(unitsHeading, unitGrid);
        units.setPadding(false);
        units.setFlexGrow(1, unitGrid);
        units.setSizeFull();

        var content = new VerticalLayout(figures, units);
        content.setFlexGrow(1, units);
        content.setSizeFull();

        setPadding(false);
        setSpacing(false);
        add(toolbar, content);
        setFlexGrow(1, content);
    }

    private static Card createFigure(String label, Span value) {
        value.addClassName("dashboard-figure-value");
        var card = new Card();
        card.addClassName("dashboard-figure");
        card.setTitle(label, 3);
        card.add(value);
        return card;
    }

    private void refresh() {
        var figures = dashboardService.getFigures();
        activeAssignments.setText(String.valueOf(figures.activeAssignments()));
        openReports.setText(String.valueOf(figures.openReports()));
        unitGrid.setItems(figures.units());
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        var ui = attachEvent.getUI();
        ui.setPollInterval(REFRESH_INTERVAL_MS);
        pollRegistration = ui.addPollListener(event -> refresh());
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        refresh();
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (pollRegistration != null) {
            pollRegistration.remove();
            pollRegistration = null;
        }
        detachEvent.getUI().setPollInterval(-1);
        super.onDetach(detachEvent);
    }

    @Override
    public String getPageTitle() {
        return "iDispatch";
    }
}
