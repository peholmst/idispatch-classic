package net.pkhapps.idispatch.server.dispatch;

import static java.util.Objects.requireNonNull;

/**
 * A destination that a runboard at the station picks the notifications up from, over a WebSocket.
 */
public record RunboardDestination(DestinationId id, long version, RunboardDestinationDetails details, boolean active)
        implements Destination {

    public RunboardDestination {
        requireNonNull(id);
        requireNonNull(details);
    }

    public RunboardKey runboardKey() {
        return details.runboardKey();
    }

    /**
     * Returns this destination with the given details, to be saved as a change of this version.
     */
    public RunboardDestination withDetails(RunboardDestinationDetails details) {
        return new RunboardDestination(id, version, details, active);
    }
}
