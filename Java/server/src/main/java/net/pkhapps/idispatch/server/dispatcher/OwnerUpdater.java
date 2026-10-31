package net.pkhapps.idispatch.server.dispatcher;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.UIDetachedException;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.server.VaadinSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Updates a UI in response to an application event. Events are published synchronously by whichever user caused
 * them, so the listeners of every open dispatcher view run in that user's thread. This class makes such an update
 * run with the UI's own session locked, with the UI and its session current, and as the user who owns the UI rather
 * than the user who caused the event.
 * <p>
 * The UI must be current for its signals: an effect runs right away only when its own UI is the current one, and is
 * otherwise handed to the effect dispatcher to run later.
 */
final class OwnerUpdater {

    private final UI ui;
    private final SecurityContext securityContext;

    private OwnerUpdater(UI ui, SecurityContext securityContext) {
        this.ui = ui;
        this.securityContext = securityContext;
    }

    /**
     * Creates an updater for the given UI, owned by the user of the current request.
     */
    static OwnerUpdater forCurrentUser(UI ui) {
        var securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(SecurityContextHolder.getContext().getAuthentication());
        return new OwnerUpdater(ui, securityContext);
    }

    /**
     * Runs the command right away if the current thread already holds the UI's session lock, which is the case when
     * the user's own action caused the event, so that the view is up to date when the action returns. Otherwise the
     * command is queued with {@link UI#access(Command)}, which never blocks on another session's lock.
     * <p>
     * When it runs right away, the UI that caused the event may be another window than this one, so the command
     * makes this UI current itself. {@link VaadinSession#accessSynchronously(Command)} would do the same, but refuses
     * to run while another session is locked, which a test that drives several users does.
     */
    void update(Command command) {
        Command asOwner = () -> {
            var originalSecurityContext = SecurityContextHolder.getContext();
            var originalUI = UI.getCurrent();
            var originalSession = VaadinSession.getCurrent();
            SecurityContextHolder.setContext(securityContext);
            VaadinSession.setCurrent(ui.getSession());
            UI.setCurrent(ui);
            try {
                command.execute();
            } finally {
                UI.setCurrent(originalUI);
                VaadinSession.setCurrent(originalSession);
                SecurityContextHolder.setContext(originalSecurityContext);
            }
        };
        var session = ui.getSession();
        if (session == null) {
            return; // The UI was closed after the event was published; there is nothing left to update
        }
        if (session.hasLock()) {
            asOwner.execute();
        } else {
            try {
                ui.access(asOwner);
            } catch (UIDetachedException ex) {
                // The UI was closed after the event was published; there is nothing left to update
            }
        }
    }
}
