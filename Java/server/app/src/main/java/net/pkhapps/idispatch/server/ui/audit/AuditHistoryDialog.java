package net.pkhapps.idispatch.server.ui.audit;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.function.SerializableFunction;
import net.pkhapps.idispatch.server.shared.audit.AuditEvent;
import net.pkhapps.idispatch.server.ui.Formatters;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Lists every event of an entity, newest first: when, who and what.
 */
class AuditHistoryDialog extends Dialog {

    /**
     * @param events     the events, newest first
     * @param partLabels names the part of the entity that an event is about, or gives {@code null} to leave it unnamed
     */
    AuditHistoryDialog(List<AuditEvent> events, SerializableFunction<String, @Nullable String> partLabels) {
        setHeaderTitle(getTranslation("audit.history"));
        setWidth("36rem");
        setMaxWidth("100%");

        if (events.isEmpty()) {
            add(new Span(getTranslation("audit.no-history")));
        } else {
            var grid = new Grid<AuditEvent>();
            grid.addThemeVariants(GridVariant.ROW_STRIPES);
            grid.setAllRowsVisible(true);
            grid.setMaxHeight("60vh");
            grid.addColumn(event -> Formatters.formatDateTime(event.timestamp()))
                    .setHeader(getTranslation("audit.time")).setAutoWidth(true).setFlexGrow(0);
            grid.addColumn(event -> event.user().displayName()).setHeader(getTranslation("audit.user"));
            grid.addColumn(event -> describeKind(event, partLabels)).setHeader(getTranslation("audit.event"));
            grid.setItems(events);
            add(grid);
        }

        getFooter().add(new Button(getTranslation("audit.close"), event -> close()));
    }

    private String describeKind(AuditEvent event, SerializableFunction<String, @Nullable String> partLabels) {
        var kind = getTranslation("audit.kind." + event.kind().name());
        var part = event.part() == null ? null : partLabels.apply(event.part());
        return part == null ? kind : getTranslation("audit.kind-of-part", kind, part);
    }
}
