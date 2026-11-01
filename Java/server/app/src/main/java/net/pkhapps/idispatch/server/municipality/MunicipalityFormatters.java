package net.pkhapps.idispatch.server.municipality;

import net.pkhapps.idispatch.server.ui.Formatters;
import org.jspecify.annotations.Nullable;

public final class MunicipalityFormatters {
    private MunicipalityFormatters() {
    }

    public static @Nullable String formatMunicipality(@Nullable MunicipalityListItem municipality) {
        return municipality == null ? null : municipality.name(Formatters.currentLocale());
    }
}
