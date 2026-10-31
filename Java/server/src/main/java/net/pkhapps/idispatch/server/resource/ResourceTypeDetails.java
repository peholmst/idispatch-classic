package net.pkhapps.idispatch.server.resource;

import net.pkhapps.idispatch.server.shared.BilingualText;

import static java.util.Objects.requireNonNull;

/**
 * What an administrator enters for a {@link ResourceType}: everything about it except its identity, version and whether
 * it has been deleted.
 */
record ResourceTypeDetails(ResourceTypeCode code, BilingualText description) {

    public ResourceTypeDetails {
        requireNonNull(code);
        requireNonNull(description);
    }
}
