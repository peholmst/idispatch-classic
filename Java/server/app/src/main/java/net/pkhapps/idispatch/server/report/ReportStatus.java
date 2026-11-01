package net.pkhapps.idispatch.server.report;

import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.signals.Signal;

/**
 * Whether a reviewer has closed a report. Shown the same way in the list of assignments and in the report itself.
 */
enum ReportStatus {
    OPEN("report.status.open"),
    CLOSED("report.status.closed");

    private final String labelKey;

    ReportStatus(String labelKey) {
        this.labelKey = labelKey;
    }

    static ReportStatus of(boolean closed) {
        return closed ? CLOSED : OPEN;
    }

    String getLabel() {
        return I18NProvider.translate(labelKey);
    }

    Badge createBadge() {
        var badge = new Badge(getLabel());
        badge.setThemeVariant(BadgeVariant.SUCCESS, this == CLOSED);
        return badge;
    }

    /**
     * Binds the badge to show the status that the signal holds.
     */
    static void bind(Badge badge, Signal<ReportStatus> status) {
        badge.bindText(status.map(ReportStatus::getLabel));
        badge.bindThemeVariant(BadgeVariant.SUCCESS, status.map(CLOSED::equals));
    }
}
