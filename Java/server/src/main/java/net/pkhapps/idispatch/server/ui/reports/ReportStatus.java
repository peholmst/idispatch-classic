package net.pkhapps.idispatch.server.ui.reports;

import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;

/**
 * Whether a reviewer has closed a report. Shown the same way in the list of assignments and in the report itself.
 */
enum ReportStatus {
    OPEN("Open"),
    CLOSED("Closed");

    private final String label;

    ReportStatus(String label) {
        this.label = label;
    }

    static ReportStatus of(boolean closed) {
        return closed ? CLOSED : OPEN;
    }

    String getLabel() {
        return label;
    }

    Badge createBadge() {
        var badge = new Badge();
        applyTo(badge);
        return badge;
    }

    void applyTo(Badge badge) {
        badge.setText(label);
        badge.setThemeVariant(BadgeVariant.SUCCESS, this == CLOSED);
    }
}
