package net.pkhapps.idispatch.server.assignment;

import net.pkhapps.idispatch.server.assignmenttype.AssignmentTypeId;
import net.pkhapps.idispatch.server.municipality.MunicipalityId;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

import static com.google.common.base.Strings.nullToEmpty;
import static java.util.Objects.requireNonNull;

/**
 * An assignment that a dispatcher handles, from being opened until it is closed. It is filled in bit by bit, so
 * everything a dispatcher enters may still be missing; {@link #missingForDispatch()} tells what resources cannot be
 * dispatched without.
 *
 * @param version incremented on every change, so that a change based on an older version can be refused
 * @param closed  when the assignment was closed, or {@code null} while it is open
 */
public record Assignment(AssignmentId id, long version, Instant opened, @Nullable Instant closed,
                         AssignmentUrgency urgency, @Nullable AssignmentTypeId typeId,
                         @Nullable MunicipalityId municipalityId, String address, String description) {

    public Assignment {
        requireNonNull(id);
        requireNonNull(opened);
        requireNonNull(urgency);
        address = nullToEmpty(address);
        description = nullToEmpty(description);
    }

    public boolean isOpen() {
        return closed == null;
    }

    public boolean isClosed() {
        return closed != null;
    }

    /**
     * Returns what the assignment still lacks before resources can be dispatched to it, or nothing when it is ready.
     */
    public Set<MissingForDispatch> missingForDispatch() {
        var missing = EnumSet.noneOf(MissingForDispatch.class);
        if (typeId == null) {
            missing.add(MissingForDispatch.TYPE);
        }
        if (municipalityId == null) {
            missing.add(MissingForDispatch.MUNICIPALITY);
        }
        if (address.isBlank()) {
            missing.add(MissingForDispatch.ADDRESS);
        }
        return missing;
    }

    public Assignment withUrgency(AssignmentUrgency urgency) {
        return new Assignment(id, version, opened, closed, urgency, typeId, municipalityId, address, description);
    }

    public Assignment withTypeId(@Nullable AssignmentTypeId typeId) {
        return new Assignment(id, version, opened, closed, urgency, typeId, municipalityId, address, description);
    }

    public Assignment withMunicipalityId(@Nullable MunicipalityId municipalityId) {
        return new Assignment(id, version, opened, closed, urgency, typeId, municipalityId, address, description);
    }

    public Assignment withAddress(String address) {
        return new Assignment(id, version, opened, closed, urgency, typeId, municipalityId, address, description);
    }

    public Assignment withDescription(String description) {
        return new Assignment(id, version, opened, closed, urgency, typeId, municipalityId, address, description);
    }
}
