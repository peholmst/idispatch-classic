package net.pkhapps.idispatch.server.resource;

import jakarta.persistence.*;

/**
 * Entity that contains past states of a {@link Resource}.
 */
@Entity
@Table(name = "resource_status_archive", indexes = @Index(columnList = "ts"))
public class ArchivedResourceStatus extends AbstractResourceStateChange {

    protected ArchivedResourceStatus() {
    }

    public ArchivedResourceStatus(ResourceStatus status) {
        setResourceId(status.getResourceId());
        setTimestamp(status.getTimestamp());
        setAssignmentId(status.getAssignmentId());
        setState(status.getState());
    }
}
