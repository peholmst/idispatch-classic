package net.pkhapps.idispatch.server.ui.audit;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.i18n.I18NProvider;
import net.pkhapps.idispatch.server.shared.audit.AuditEvent;
import net.pkhapps.idispatch.server.shared.audit.AuditLog;
import net.pkhapps.idispatch.server.shared.audit.AuditedEntity;
import net.pkhapps.idispatch.server.ui.Formatters;
import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * Tells who last did something to an entity and when, from the {@link AuditLog}: who created it if that is all that
 * has happened, otherwise the newest event of any kind, such as an edit or a deletion. A button beside it opens the
 * whole history in a dialog. Shows nothing while there is no entity, such as while a new one is being added.
 * <p>
 * The trail reads the log when it is given the entity; call {@link #show(AuditedEntity)} again after the entity has
 * changed.
 */
public class AuditTrail extends Composite<HorizontalLayout> {

    private final AuditLog auditLog;
    private final Span summary = new Span();
    private @Nullable AuditedEntity entity;
    private SerializableFunction<String, @Nullable String> partLabels = part -> null;

    public AuditTrail(AuditLog auditLog) {
        this.auditLog = requireNonNull(auditLog);
        var history = new Button(VaadinIcon.TIME_BACKWARD.create(), event -> openHistory());
        history.addThemeVariants(ButtonVariant.TERTIARY, ButtonVariant.SMALL);
        history.setAriaLabel(getTranslation("audit.show-history"));
        history.setTooltipText(getTranslation("audit.show-history"));

        var layout = getContent();
        layout.addClassName("audit-trail");
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        layout.add(summary, history);
        layout.setVisible(false);
    }

    /**
     * Shows the trail of the given entity, read from the log now, or nothing if the entity is {@code null}.
     */
    public void show(@Nullable AuditedEntity entity) {
        this.entity = entity;
        getContent().setVisible(entity != null);
        summary.setText(entity == null ? "" : auditLog.findLatest(entity).map(AuditTrail::describe)
                .orElseGet(() -> getTranslation("audit.no-history")));
    }

    /**
     * Sets how the history names the parts of the entity that its events are about, such as a unit's part of a report.
     * A part that the function gives no label for is not named.
     */
    public void setPartLabels(SerializableFunction<String, @Nullable String> partLabels) {
        this.partLabels = requireNonNull(partLabels);
    }

    /**
     * Tells who did what and when, such as "Last edited by Erik Explorer on 5.11.2026 14.03".
     */
    public static String describe(AuditEvent event) {
        return I18NProvider.translate("audit.summary." + event.kind().name(), event.user().displayName(),
                Formatters.formatDateTime(event.timestamp()));
    }

    private void openHistory() {
        if (entity != null) {
            new AuditHistoryDialog(auditLog.findEvents(entity), partLabels).open();
        }
    }
}
