package net.pkhapps.idispatch.server.dispatch;

import jakarta.persistence.*;
import net.pkhapps.idispatch.server.assignment.Assignment;
import net.pkhapps.idispatch.server.assignment.AssignmentId;
import net.pkhapps.idispatch.server.assignment.AssignmentUrgency;
import net.pkhapps.idispatch.server.assignmenttype.AssignmentTypeId;
import net.pkhapps.idispatch.server.municipality.MunicipalityId;
import net.pkhapps.idispatch.server.resource.ResourceId;
import net.pkhapps.idispatch.server.shared.AbstractTimestampedEntity;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Collections.unmodifiableSet;

/**
 * TODO Document me
 */
@Entity
@Table(name = "dispatch_notifications")
public class DispatchNotification extends AbstractTimestampedEntity {

    @ElementCollection
    @CollectionTable(name = "dispatch_notification_assigned_resources",
            joinColumns = @JoinColumn(name = "dispatch_notification_id"))
    @Column(name = "resource_id", nullable = false)
    private Set<Long> assignedResources;

    @ManyToMany
    @JoinTable(name = "dispatch_notification_destinations",
            joinColumns = @JoinColumn(name = "dispatch_notification_id"),
            inverseJoinColumns = @JoinColumn(name = "destination_id"))
    private Set<Destination> destinations;

    @Column(name = "assignment_id", nullable = false)
    private Long assignmentId;

    @Column(name = "assignment_municipality_id", nullable = false)
    private Long municipalityId;

    @Column(name = "assignment_address", nullable = false)
    private String address;

    @Column(name = "assignment_type_id", nullable = false)
    private Long assignmentTypeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_urgency", nullable = false)
    private AssignmentUrgency urgency;

    @Column(name = "assignment_description", nullable = false)
    private String description;

    protected DispatchNotification() {
    }

    public DispatchNotification(Assignment assignment, Collection<ResourceId> assignedResources, Collection<Destination> destinations) {
        this.assignedResources = checkNotNull(assignedResources).stream().map(ResourceId::value).collect(Collectors.toSet());
        this.destinations = newHashSet(checkNotNull(destinations));
        this.assignmentId = checkNotNull(assignment).id().value();
        municipalityId = checkNotNull(assignment.municipalityId(), "The assignment has no municipality").value();
        address = assignment.address();
        assignmentTypeId = checkNotNull(assignment.typeId(), "The assignment has no type").value();
        urgency = assignment.urgency();
        description = assignment.description();
    }

    public Set<ResourceId> getAssignedResources() {
        return assignedResources.stream().map(ResourceId::new).collect(Collectors.toUnmodifiableSet());
    }

    public Set<Destination> getDestinations() {
        return unmodifiableSet(destinations);
    }

    public <D extends Destination> Set<D> getDestinationsOfType(Class<D> type) {
        return destinations.stream().filter(type::isInstance).map(type::cast).collect(Collectors.toSet());
    }

    public AssignmentId getAssignmentId() {
        return new AssignmentId(assignmentId);
    }

    public MunicipalityId getMunicipalityId() {
        return new MunicipalityId(municipalityId);
    }

    public String getAddress() {
        return address;
    }

    public AssignmentTypeId getAssignmentTypeId() {
        return new AssignmentTypeId(assignmentTypeId);
    }

    public AssignmentUrgency getUrgency() {
        return urgency;
    }

    public String getDescription() {
        return description;
    }
}
