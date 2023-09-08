package net.pkhapps.idispatch.server.ui;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.RouterLayout;
import net.pkhapps.idispatch.server.ui.dws.DispatchView;
import net.pkhapps.idispatch.server.ui.management.MunicipalityManagementView;
import net.pkhapps.idispatch.server.ui.reports.ReportsView;

public class RootLayout extends AppLayout implements RouterLayout, AfterNavigationObserver {

    private final HorizontalLayout navbarLayout;
    private final H2 viewTitle;

    public RootLayout() {
        var toggle = new DrawerToggle();
        var title = new H1("iDispatch");
        title.getStyle()
                .set("font-size", "var(--lumo-font-size-l)")
                .set("line-height", "var(--lumo-size-l)")
                .set("margin", "0 var(--lumo-space-m)");

        var views = getPrimaryNavigation();

        addToDrawer(title, views);

        viewTitle = new H2();
        viewTitle.getStyle()
                .set("font-size", "var(--lumo-font-size-l)")
                .set("line-height", "var(--lumo-size-l)")
                .set("margin", "0");

        navbarLayout = new HorizontalLayout();
        navbarLayout.setPadding(false);
        navbarLayout.setSpacing(false);
        navbarLayout.setWidthFull();
        navbarLayout.add(toggle, viewTitle);

        addToNavbar(navbarLayout);

        setPrimarySection(Section.DRAWER);
    }

    private Tabs getPrimaryNavigation() {
        var tabs = new Tabs();
        tabs.add(
                // TODO Filter by role
                TabUtils.createNavigationTab(VaadinIcon.HOME, "Home", RootView.class),
                TabUtils.createNavigationTab(VaadinIcon.HEADSET, "Dispatcher", DispatchView.class),
                TabUtils.createNavigationTab(VaadinIcon.COG, "Management", MunicipalityManagementView.class),
                TabUtils.createNavigationTab(VaadinIcon.CHART, "Reports", ReportsView.class)
        );
        tabs.setOrientation(Tabs.Orientation.VERTICAL);
        tabs.setSelectedIndex(1);
        return tabs;
    }

    @Override
    public void afterNavigation(AfterNavigationEvent afterNavigationEvent) {
        afterNavigationEvent.getActiveChain()
                .stream()
                .filter(HasNavbarContent.class::isInstance)
                .map(HasNavbarContent.class::cast)
                .findFirst()
                .ifPresentOrElse(this::setNavbarContent, this::clearNavbarContent);
    }

    private void setNavbarContent(HasNavbarContent contentProvider) {
        clearNavbarContent();
        navbarLayout.add(contentProvider.getNavbarContent());
        viewTitle.setText(contentProvider.getTitle());
    }

    private void clearNavbarContent() {
        if (navbarLayout.getComponentCount() > 2) {
            navbarLayout.remove(navbarLayout.getComponentAt(2));
        }
        viewTitle.setText("");
    }
}
