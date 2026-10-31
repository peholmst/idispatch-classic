package net.pkhapps.idispatch.server.resource;

import net.pkhapps.idispatch.server.shared.BilingualText;

import static java.util.Objects.requireNonNull;

/**
 * A type of resource, such as an ambulance. Resources refer to their type by its {@link ResourceTypeId}.
 *
 * @param version incremented on every change, so that a change based on an older version can be refused
 * @param active  {@code false} once the type has been deleted; deleted types are kept for the resources that have them
 */
public record ResourceType(ResourceTypeId id, long version, ResourceTypeCode code, BilingualText description,
                           boolean active) {

    public ResourceType {
        requireNonNull(id);
        requireNonNull(code);
        requireNonNull(description);
    }

    public ResourceTypeDetails details() {
        return new ResourceTypeDetails(code, description);
    }

    /**
     * Returns this type with the given details, to be saved as a change of this version.
     */
    public ResourceType withDetails(ResourceTypeDetails details) {
        return new ResourceType(id, version, details.code(), details.description(), active);
    }
}
