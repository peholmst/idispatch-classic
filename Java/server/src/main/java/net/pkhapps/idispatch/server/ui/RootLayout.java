package net.pkhapps.idispatch.server.ui;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;
import net.pkhapps.idispatch.server.security.Roles;
import net.pkhapps.idispatch.server.ui.dws.DispatchView;
import net.pkhapps.idispatch.server.ui.management.ManagementLayout;
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
                .set("font-size", "var(--aura-font-size-l)")
                .set("line-height", "var(--aura-line-height-xl)")
                .set("margin", "0 var(--vaadin-padding-m)");

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

        var navigation = new VerticalLayout(createViewNavigation());
        if (isUserInRole(Roles.ROLE_ADMIN)) {
            navigation.add(createAdministrationNavigation());
        }
        navigation.add(createAccountNavigation(authenticationContext));

        addToDrawer(header, new Scroller(navigation));

        viewTitle = new H2();
        viewTitle.getStyle()
                .set("font-size", "var(--aura-font-size-l)")
                .set("line-height", "var(--aura-line-height-xl)")
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

    private SideNav createViewNavigation() {
        var nav = new SideNav();
        nav.addItem(new SideNavItem(getTranslation("nav.home"), RootView.class, VaadinIcon.HOME.create()));
        if (isUserInRole(Roles.ROLE_DISPATCHER)) {
            nav.addItem(new SideNavItem(getTranslation("nav.dispatcher"), DispatchView.class, VaadinIcon.HEADSET.create()));
        }
        if (isUserInRole(Roles.ROLE_REPORT_READER) || isUserInRole(Roles.ROLE_REPORT_WRITER)
                || isUserInRole(Roles.ROLE_REPORT_REVIEWER)) {
            nav.addItem(new SideNavItem(getTranslation("nav.reports"), ReportsView.class, VaadinIcon.CHART.create()));
        }
        return nav;
    }

    private SideNav createAdministrationNavigation() {
        var management = new SideNavItem(getTranslation("nav.management"), MunicipalityManagementView.class, VaadinIcon.COG.create());
        // Highlight the item in every section of the management console, not only in the one it opens
        var routes = RouteConfiguration.forApplicationScope();
        management.setPathAliases(ManagementLayout.getSectionViews().stream()
                .map(routes::getUrl)
                .collect(Collectors.toSet()));

        var nav = new SideNav(getTranslation("nav.administration"));
        nav.addItem(management);
        return nav;
    }

    private SideNav createAccountNavigation(AuthenticationContext authenticationContext) {
        var nav = new SideNav(getTranslation("nav.account"));
        // Users are managed in Keycloak, and change their password in the Keycloak account console of the realm
        // that issued their ID token
        authenticationContext.getAuthenticatedUser(Object.class)
                .filter(OidcUser.class::isInstance)
                .map(OidcUser.class::cast)
                .ifPresent(user -> {
                    var account = new SideNavItem(getTranslation("nav.my-account"), user.getIssuer() + "/account",
                            VaadinIcon.USER.create());
                    account.setOpenInNewBrowserTab(true);
                    nav.addItem(account);
                });

        // Logging out is an action, not a link. The item still gets a path so that it is a focusable link like the
        // others, but the router ignores it and the click never follows it.
        var logout = new SideNavItem(getTranslation("nav.log-out"), "logout", VaadinIcon.SIGN_OUT.create());
        logout.setRouterIgnore(true);
        logout.getElement().addEventListener("click", event -> authenticationContext.logout()).preventDefault();
        nav.addItem(logout);
        return nav;
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
