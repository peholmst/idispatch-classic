package net.pkhapps.idispatch.server.ui;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.spring.security.AuthenticationContext;
import net.pkhapps.idispatch.server.security.Roles;
import net.pkhapps.idispatch.server.ui.dws.DispatchView;
import net.pkhapps.idispatch.server.ui.management.MunicipalityManagementView;
import net.pkhapps.idispatch.server.ui.reports.ReportsView;
import org.springframework.security.core.context.SecurityContextHolder;

public class RootLayout extends AppLayout implements RouterLayout, AfterNavigationObserver {

    private final HorizontalLayout navbarLayout;
    private final H2 viewTitle;

    public RootLayout(AuthenticationContext authenticationContext) {
        var toggle = new DrawerToggle();
        var title = new H1("iDispatch");
        title.getStyle()
                .set("font-size", "var(--lumo-font-size-l)")
                .set("line-height", "var(--lumo-size-l)")
                .set("margin", "0 var(--lumo-space-m)");

        var logout = new Button(VaadinIcon.POWER_OFF.create(), event -> authenticationContext.logout());
        var user = new HorizontalLayout(new Span(authenticationContext.getPrincipalName().orElse("")), logout);
        user.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        user.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        user.setWidthFull();
        user.setPadding(true);

        var header = new VerticalLayout(title, user);
        header.setSizeUndefined();
        header.setPadding(false);
        header.setSpacing(false);

        var views = getPrimaryNavigation();

        addToDrawer(header, views);

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
        tabs.add(TabUtils.createNavigationTab(VaadinIcon.HOME, "Home", RootView.class));
        if (isUserInRole(Roles.ROLE_DISPATCHER)) {
            tabs.add(TabUtils.createNavigationTab(VaadinIcon.HEADSET, "Dispatcher", DispatchView.class));
        }
        if (isUserInRole(Roles.ROLE_REPORT_READER)) {
            tabs.add(TabUtils.createNavigationTab(VaadinIcon.CHART, "Reports", ReportsView.class));
        }
        if (isUserInRole(Roles.ROLE_ADMIN)) {
            tabs.getElement().appendChild(ElementFactory.createHr());
            tabs.add(TabUtils.createNavigationTab(VaadinIcon.COG, "Management", MunicipalityManagementView.class));
        }
        tabs.getElement().appendChild(ElementFactory.createHr());
        tabs.add(TabUtils.createNavigationTab(VaadinIcon.PASSWORD, "Change Password", ChangePasswordView.class));

        tabs.setOrientation(Tabs.Orientation.VERTICAL);
        tabs.setSelectedIndex(1);
        return tabs;
    }

    private boolean isUserInRole(String role) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            return auth.getAuthorities().stream().anyMatch(ga -> ga.getAuthority().equals(role));
        }
        return false;
    }

    @Override
    public void afterNavigation(AfterNavigationEvent afterNavigationEvent) {
        afterNavigationEvent.getActiveChain()
                .stream()
                .filter(HasNavbarContent.class::isInstance)
                .map(HasNavbarContent.class::cast)
                .findFirst()
                .ifPresentOrElse(this::setNavbarContent, this::clearNavbarContent);
        // TODO Select correct tab
    }

    private void setNavbarContent(HasNavbarContent contentProvider) {
        clearNavbarContent();
        contentProvider.getNavbarContent().ifPresent(navbarLayout::add);
        viewTitle.setText(contentProvider.getTitle());
    }

    private void clearNavbarContent() {
        if (navbarLayout.getComponentCount() > 2) {
            navbarLayout.remove(navbarLayout.getComponentAt(2));
        }
        viewTitle.setText("");
    }
}
