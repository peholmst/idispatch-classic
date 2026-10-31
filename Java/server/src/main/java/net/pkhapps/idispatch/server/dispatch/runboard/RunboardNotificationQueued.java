package net.pkhapps.idispatch.server.dispatch.runboard;

import org.springframework.context.ApplicationEvent;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Event published when a dispatch notification is waiting to be picked up by a runboard.
 */
public class RunboardNotificationQueued extends ApplicationEvent {

    private final String runboardKey;

    public RunboardNotificationQueued(Object source, String runboardKey) {
        super(source);
        this.runboardKey = checkNotNull(runboardKey);
    }

    public String getRunboardKey() {
        return runboardKey;
    }
}
