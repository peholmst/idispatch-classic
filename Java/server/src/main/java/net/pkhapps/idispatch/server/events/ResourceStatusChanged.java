package net.pkhapps.idispatch.server.events;

import net.pkhapps.idispatch.server.entity.ResourceStatus;
import org.springframework.context.ApplicationEvent;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Event published when the state of a resource is changed.
 */
public class ResourceStatusChanged extends ApplicationEvent {

    private final ResourceStatus resourceStatus;

    public ResourceStatusChanged(Object source, ResourceStatus resourceStatus) {
        super(source);
        this.resourceStatus = checkNotNull(resourceStatus);
    }

    public ResourceStatus getResourceStatus() {
        return resourceStatus;
    }
}
