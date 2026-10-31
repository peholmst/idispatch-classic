package net.pkhapps.idispatch.server.municipality;

import net.pkhapps.idispatch.server.ui.Formatters;

public final class MunicipalityFormatters {
    private MunicipalityFormatters() {
    }

    public static String formatMunicipality(Municipality municipality) {
        return municipality == null ? null : municipality.getName(Formatters.currentLocale());
    }
}
