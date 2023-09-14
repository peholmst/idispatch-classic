package net.pkhapps.idispatch.server.ui;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.server.ErrorEvent;
import com.vaadin.flow.server.ErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Optional;

public class CustomErrorHandler implements ErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(CustomErrorHandler.class);
    private final SecureRandom rnd;

    public CustomErrorHandler() {
        try {
            this.rnd = SecureRandom.getInstanceStrong();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Could not initialize random number generator", ex);
        }
    }

    @Override
    public void error(ErrorEvent event) {
        if (event.getThrowable() instanceof AccessDeniedException) {
            showAccessDenied(event.getThrowable().getMessage());
        } else {
            var errorId = generateEntryId();
            log.error("Unhandled exception [" + errorId + "]", event.getThrowable());
            Optional.ofNullable(UI.getCurrent()).ifPresent(ui -> ui.access(() -> showNotification(errorId)));
        }
    }

    private void showAccessDenied(String additionalMessage) {
        var notification = createNotification(
                VaadinIcon.KEY.create(),
                "Access has been denied",
                new Div(new Text(additionalMessage)),
                NotificationVariant.LUMO_WARNING
        );
        notification.addThemeVariants(NotificationVariant.LUMO_WARNING);
        notification.open();
    }

    private void showNotification(String errorId) {
        var errorSpan = new Span(errorId);
        errorSpan.getStyle()
                .set("background-color", "var(--lumo-contrast-20pct)")
                .set("border-radius", "var(--lumo-border-radius-s)")
                .set("padding", "var(--lumo-space-xs)");
        var text = new Div(new Text("Please contact the administrator and give them this error ID: "), errorSpan);
        text.getStyle().set("font-size", "var(--lumo-font-size-s)");

        var notification = createNotification(
                VaadinIcon.WARNING.create(),
                "An unexpected error has occurred",
                text,
                NotificationVariant.LUMO_ERROR
        );
        notification.open();
    }

    private Notification createNotification(Icon icon, String header, Div text, NotificationVariant variant) {
        var notification = new Notification();
        notification.addThemeVariants(variant);

        var headerDiv = new Div(new Text(header));
        headerDiv.getStyle().set("font-weight", "600");
        text.getStyle().set("font-size", "var(--lumo-font-size-s)");
        var info = new Div(headerDiv, text);
        var closeBtn = new Button(VaadinIcon.CLOSE_SMALL.create(),
                clickEvent -> notification.close());
        closeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);

        var layout = new HorizontalLayout(icon, info, closeBtn);
        layout.setAlignItems(FlexComponent.Alignment.CENTER);

        notification.add(layout);
        notification.setPosition(Notification.Position.MIDDLE);

        return notification;
    }

    private String generateEntryId() {
        var buf = new byte[8];
        rnd.nextBytes(buf);
        return HexFormat.ofDelimiter(":").withUpperCase().formatHex(buf);
    }
}
