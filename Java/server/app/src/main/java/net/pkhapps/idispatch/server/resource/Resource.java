package net.pkhapps.idispatch.server.resource;

import static java.util.Objects.requireNonNull;

/**
 * A unit that dispatchers send out, such as an ambulance. Its current state is kept in its {@link ResourceStatus}.
 *
 * @param version incremented on every change, so that a change based on an older version can be refused
 * @param active  {@code false} once the resource has been deleted; deleted resources are kept for the history that
 *                refers to them
 */
public record Resource(ResourceId id, long version, CallSign callSign, ResourceTypeId typeId, boolean active) {

    public Resource {
        requireNonNull(id);
        requireNonNull(callSign);
        requireNonNull(typeId);
    }

    public ResourceDetails details() {
        return new ResourceDetails(callSign, typeId);
    }

    /**
     * Returns this resource with the given details, to be saved as a change of this version.
     */
    public Resource withDetails(ResourceDetails details) {
        return new Resource(id, version, details.callSign(), details.typeId(), active);
    }
}
