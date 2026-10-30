package net.pkhapps.idispatch.server.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.ScrollerVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;
import net.pkhapps.idispatch.server.security.Roles;
import net.pkhapps.idispatch.server.ui.dws.DispatchView;
import net.pkhapps.idispatch.server.ui.management.AssignmentTypeManagementView;
import net.pkhapps.idispatch.server.ui.management.DestinationManagementView;
import net.pkhapps.idispatch.server.ui.management.MunicipalityManagementView;
import net.pkhapps.idispatch.server.ui.management.ResourceManagementView;
import net.pkhapps.idispatch.server.ui.management.ResourceTypeManagementView;
import net.pkhapps.idispatch.server.ui.reports.ReportsView;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.stream.Collectors;
import java.util.stream.Stream;

@PermitAll
public class RootLayout extends AppLayout implements RouterLayout {

    public RootLayout(AuthenticationContext authenticationContext) {
        var navigation = new VerticalLayout(createViewNavigation());
        navigation.setPadding(false);
        navigation.setMinWidth(200, Unit.PIXELS);
        if (isUserInRole(Roles.ROLE_ADMIN)) {
            navigation.add(createAdministrationNavigation());
        }
        navigation.add(createAccountNavigation(authenticationContext));
        var drawer = new Scroller(navigation);
        drawer.addThemeVariants(ScrollerVariant.OVERFLOW_INDICATORS);

        addToDrawer(createApplicationHeader(), drawer, createUserFooter(getDisplayName(authenticationContext)));

        setPrimarySection(Section.DRAWER);
    }

    private static Component createApplicationHeader() {
        var logo = new Avatar("iDispatch");
        logo.setAbbreviation("iD");
        logo.addClassName("app-logo");
        logo.addThemeVariants(AvatarVariant.AURA_FILLED, AvatarVariant.XSMALL);

        var name = new Span("iDispatch");
        name.addClassName("app-name");

        var header = new HorizontalLayout(logo, name);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setPadding(true);
        return header;
    }

    private static Component createUserFooter(String displayName) {
        var avatar = new Avatar(displayName);
        avatar.addClassName("app-user");
        avatar.addThemeVariants(AvatarVariant.XSMALL);
        var name = new Span(displayName);
        name.addClassName("app-user-name");

        var footer = new HorizontalLayout(avatar, name);
        footer.setAlignItems(FlexComponent.Alignment.CENTER);
        footer.setPadding(true);
        footer.addClassName("app-footer");
        return footer;
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
        nav.setWidthFull();
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
        var nav = new SideNav(getTranslation("nav.administration"));
        nav.setWidthFull();
        nav.addItem(
                new SideNavItem(getTranslation("management.municipalities"), MunicipalityManagementView.class, VaadinIcon.BUILDING.create()),
                new SideNavItem(getTranslation("management.assignment-types"), AssignmentTypeManagementView.class, VaadinIcon.CLIPBOARD_TEXT.create()),
                new SideNavItem(getTranslation("management.resources"), ResourceManagementView.class, VaadinIcon.TRUCK.create()),
                new SideNavItem(getTranslation("management.resource-types"), ResourceTypeManagementView.class, VaadinIcon.TAGS.create()),
                new SideNavItem(getTranslation("management.destinations"), DestinationManagementView.class, VaadinIcon.PAPERPLANE.create())
        );
        return nav;
    }

    private SideNav createAccountNavigation(AuthenticationContext authenticationContext) {
        var nav = new SideNav(getTranslation("nav.account"));
        nav.setWidthFull();
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
}
