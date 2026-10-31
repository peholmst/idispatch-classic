package net.pkhapps.idispatch.server.dispatch;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * A destination as it is listed in the management view, flattened to what the grid shows. The call signs are those the
 * resources have now, in call sign order.
 */
public record DestinationListItem(DestinationId id, Kind kind, @Nullable String description, Locale locale,
                                  List<String> callSigns, boolean active) {

    public enum Kind {
        RUNBOARD, SMS
    }
}
