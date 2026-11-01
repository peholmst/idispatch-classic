package net.pkhapps.idispatch.server.municipality;

import net.pkhapps.idispatch.server.shared.BilingualText;

import static java.util.Objects.requireNonNull;

/**
 * A municipality that assignments are in. Its name is in Finnish and Swedish, and unique in each language. Assignments
 * refer to their municipality by its {@link MunicipalityId}.
 *
 * @param version incremented on every change, so that a change based on an older version can be refused
 * @param active  {@code false} once the municipality has been deleted; deleted municipalities are kept for the
 *                assignments that are in them
 */
public record Municipality(MunicipalityId id, long version, BilingualText name, boolean active) {

    public Municipality {
        requireNonNull(id);
        requireNonNull(name);
    }

    public MunicipalityDetails details() {
        return new MunicipalityDetails(name);
    }

    /**
     * Returns this municipality with the given details, to be saved as a change of this version.
     */
    public Municipality withDetails(MunicipalityDetails details) {
        return new Municipality(id, version, details.name(), active);
    }
}
