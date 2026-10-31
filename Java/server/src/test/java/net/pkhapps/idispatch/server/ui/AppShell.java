package net.pkhapps.idispatch.server.ui;

import com.vaadin.browserless.BrowserlessUIContext;
import com.vaadin.browserless.ComponentTester;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.server.ErrorEvent;
import com.vaadin.flow.server.VaadinSession;

import java.util.List;
import java.util.Optional;

/**
 * Where things are in the part of the application that every view shares: the drawer with the navigation and the
 * user, the title at the top of the view, notifications and confirmation questions.
 */
public class AppShell {

    protected final BrowserlessUIContext window;

    public AppShell(BrowserlessUIContext window) {
        this.window = window;
    }

    public void openHome() {
        window.navigate(RootView.class);
    }

    /**
     * Opens the given address, like typing it in the browser's address bar.
     */
    public void openAddress(String location) {
        window.activate();
        window.getUI().navigate(location);
    }

    /**
     * The title at the top of the view, or nothing if no view is shown.
     */
    public Optional<String> viewTitle() {
        var titles = window.findH2().components();
        return titles.isEmpty() ? Optional.empty() : Optional.of(titles.getFirst().getText());
    }

    public boolean showsAView() {
        window.activate();
        return !window.getUI().getInternals().getActiveRouterTargetsChain().isEmpty();
    }

    public boolean showsNotFound() {
        window.activate();
        return window.getCurrentView() instanceof com.vaadin.flow.router.RouteNotFoundError;
    }

    public String welcomeText() {
        return window.findH1().component().getText();
    }

    /**
     * The URL the browser was sent to outside the application, such as the login page.
     */
    public String externalAddress() {
        return window.getExternalNavigationURL();
    }

    public List<String> navigationItems() {
        return window.find(SideNavItem.class).all().stream().map(SideNavItem::getLabel).toList();
    }

    public void goTo(String navigationItem) {
        window.activate();
        var item = navigationItem(navigationItem);
        window.getUI().navigate(item.getPath());
    }

    public String linkOf(String navigationItem) {
        return navigationItem(navigationItem).getPath();
    }

    /**
     * Whether the browser follows the navigation item's link when it is clicked. The router ignores an item that is
     * an action, and its click listener does the work instead.
     */
    public boolean followsLink(String navigationItem) {
        return !navigationItem(navigationItem).isRouterIgnore();
    }

    public boolean opensInNewTab(String navigationItem) {
        return navigationItem(navigationItem).isOpenInNewBrowserTab();
    }

    /**
     * Clicks the navigation item the way the browser does, firing the click that its listeners receive. The side
     * navigation locator would follow the item's path instead, which the browser doesn't do for an item that the
     * router ignores, such as Log Out.
     */
    public void click(String navigationItem) {
        new ComponentTester<SideNavItem>(navigationItem(navigationItem)) {
            void clickLink() {
                fireDomEvent("click");
            }
        }.clickLink();
    }

    /**
     * The name shown next to the user's avatar at the bottom of the drawer.
     */
    public String userName() {
        var avatar = window.find(Avatar.class).withCondition(a -> a.hasClassName("app-user")).single();
        var name = avatar.getName();
        if (!window.findSpan().withText(name).exists()) {
            throw new AssertionError("The avatar is for " + name + ", but the name is not shown next to it");
        }
        return name;
    }

    private SideNavItem navigationItem(String label) {
        return window.find(SideNavItem.class).withCondition(item -> label.equals(item.getLabel())).single();
    }

    public boolean showsText(String text) {
        return window.findSpan().withText(text).exists();
    }

    /**
     * The texts of the notifications on screen, including notifications made of components.
     */
    public List<String> notifications() {
        return window.find(Notification.class).all().stream()
                .map(n -> Optional.ofNullable(n.getElement().getProperty("text"))
                        .orElseGet(() -> n.getElement().getTextRecursively()))
                .toList();
    }

    public boolean wasToldThat(String message) {
        return notifications().stream().anyMatch(text -> text.contains(message));
    }

    public boolean isAsked(String question) {
        var dialogs = window.findConfirmDialog().atIndex(1);
        return dialogs.exists() && question.equals(dialogs.getHeader());
    }

    public boolean isAskedAnything() {
        return window.findConfirmDialog().components().stream().findAny().isPresent();
    }

    public void answerConfirm() {
        window.findConfirmDialog().confirm();
    }

    public void answerCancel() {
        window.findConfirmDialog().cancel();
    }

    /**
     * Makes something go wrong with the given exception while the user works in the window. Exceptions thrown by
     * listeners in a browserless test reach the test instead of the session's error handler, so this hands the
     * exception to the error handler directly, as Vaadin does in a running application.
     */
    public void somethingFailsWith(Throwable exception) {
        window.activate();
        VaadinSession.getCurrent().getErrorHandler().error(new ErrorEvent(exception));
        window.roundTrip();
    }
}
