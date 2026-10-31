package net.pkhapps.idispatch.server.resource;

import static java.util.Objects.requireNonNull;

/**
 * What an administrator enters for a {@link Resource}: everything about it except its identity, version and whether it
 * has been deleted.
 */
record ResourceDetails(CallSign callSign, ResourceTypeId typeId) {

    public ResourceDetails {
        requireNonNull(callSign);
        requireNonNull(typeId);
    }
}
