package net.pkhapps.idispatch.server.municipality;

import net.pkhapps.idispatch.server.shared.BilingualText;

import static java.util.Objects.requireNonNull;

/**
 * What an administrator enters for a {@link Municipality}: everything about it except its identity, version and whether
 * it has been deleted.
 */
public record MunicipalityDetails(BilingualText name) {

    public MunicipalityDetails {
        requireNonNull(name);
    }
}
