package net.pkhapps.idispatch.server.ui;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;
import net.pkhapps.idispatch.server.security.Roles;
import net.pkhapps.idispatch.server.ui.dws.DispatchView;
import net.pkhapps.idispatch.server.ui.management.MunicipalityManagementView;
import net.pkhapps.idispatch.server.ui.reports.ReportsView;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.stream.Collectors;
import java.util.stream.Stream;

@PermitAll
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

        var displayName = getDisplayName(authenticationContext);
        var avatar = new Avatar(displayName);
        var name = new Span(displayName);
        name.getStyle()
                .set("overflow", "hidden")
                .set("text-overflow", "ellipsis")
                .set("white-space", "nowrap");
        var user = new HorizontalLayout(avatar, name);
        user.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        user.setWidthFull();
        user.setPadding(true);

        var header = new VerticalLayout(title, user);
        header.setSizeUndefined();
        header.setPadding(false);
        header.setSpacing(false);

        var views = getPrimaryNavigation(authenticationContext);

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

    /**
     * Returns the first and last name from the user's ID token, or the username if the token has neither.
     */
    private static String getDisplayName(AuthenticationContext authenticationContext) {
        return authenticationContext.getAuthenticatedUser(Object.class)
                .filter(OidcUser.class::isInstance)
                .map(OidcUser.class::cast)
                .map(user -> Stream.of(user.getGivenName(), user.getFamilyName())
                        .filter(part -> part != null && !part.isBlank())
                        .collect(Collectors.joining(" ")))
                .filter(fullName -> !fullName.isEmpty())
                .or(authenticationContext::getPrincipalName)
                .orElse("");
    }

    private Tabs getPrimaryNavigation(AuthenticationContext authenticationContext) {
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
        // Select the first view after Home, if the user has one
        tabs.setSelectedIndex(tabs.getComponentCount() > 1 ? 1 : 0);

        tabs.getElement().appendChild(ElementFactory.createHr());
        // Users are managed in Keycloak, and change their password in the Keycloak account console of the realm
        // that issued their ID token
        authenticationContext.getAuthenticatedUser(Object.class)
                .filter(OidcUser.class::isInstance)
                .map(OidcUser.class::cast)
                .ifPresent(user -> tabs.add(createAccountTab(user.getIssuer() + "/account")));
        var logoutTab = new Tab(createMenuItemIcon(VaadinIcon.SIGN_OUT), new Span("Log Out"));
        tabs.add(logoutTab);

        tabs.setOrientation(Tabs.Orientation.VERTICAL);
        // Selecting the tab, by clicking it or with the keyboard, logs out
        tabs.addSelectedChangeListener(event -> {
            if (event.getSelectedTab() == logoutTab) {
                authenticationContext.logout();
            }
        });
        return tabs;
    }

    private static Tab createAccountTab(String accountConsoleUrl) {
        var link = new Anchor(accountConsoleUrl, createMenuItemIcon(VaadinIcon.USER), new Span("My Account"));
        link.setTarget(AnchorTarget.BLANK);
        link.setTabIndex(-1);
        return new Tab(link);
    }

    private static Icon createMenuItemIcon(VaadinIcon vaadinIcon) {
        var icon = vaadinIcon.create();
        icon.getStyle()
                .set("box-sizing", "border-box")
                .set("margin-inline-end", "var(--lumo-space-m)")
                .set("padding", "var(--lumo-space-xs)");
        return icon;
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
