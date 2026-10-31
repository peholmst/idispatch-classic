package net.pkhapps.idispatch.server.resource;

/**
 * A resource as other features list and offer it, flattened to what they show.
 */
public record ResourceListItem(ResourceId id, String callSign, boolean active) {
}
