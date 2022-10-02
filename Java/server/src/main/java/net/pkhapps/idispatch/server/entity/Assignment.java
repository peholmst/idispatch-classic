package net.pkhapps.idispatch.server.entity;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.Instant;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.nullToEmpty;

/**
 * Entity representing a assignment.
 */
@Entity
@Table(name = "assignments")
public class Assignment extends AbstractLockableEntity {

    public static final String PROP_OPENED = "opened";
    public static final String PROP_CLOSED = "closed";
    public static final String PROP_URGENCY = "urgency";
    public static final String PROP_TYPE = "type";
    public static final String PROP_DESCRIPTION = "description";
    public static final String PROP_MUNICIPALITY = "municipality";
    public static final String PROP_ADDRESS = "address";

    @Column(name = "opened", nullable = false)
    private Instant opened;
    @Column(name = "closed")
    private Instant closed;
    @Enumerated(EnumType.STRING)
    @Column(name = "urgency", nullable = false)
    private AssignmentUrgency urgency = AssignmentUrgency.N;
    @ManyToOne
    @JoinColumn(name = "type_id")
    @NotNull(message = "Please select an assignment type", groups = DispatchValidationGroup.class)
    private AssignmentType type;
    @Column(name = "description", nullable = false)
    private String description = "";
    @ManyToOne
    @JoinColumn(name = "municipality_id")
    @NotNull(message = "Please select a municipality", groups = DispatchValidationGroup.class)
    private Municipality municipality;
    @Column(name = "address", nullable = false)
    @NotBlank(message = "Please enter an address", groups = DispatchValidationGroup.class)
    private String address = "";

    public Assignment() {
        opened = Instant.now();
    }

    public Instant getOpened() {
        return opened;
    }

    public Instant getClosed() {
        return closed;
    }

    public boolean isAssignmentOpen() {
        return closed == null;
    }

    public boolean isAssignmentClosed() {
        return closed != null;
    }

    public void setClosed(Instant closed) {
        this.closed = checkNotNull(closed);
    }

    public AssignmentUrgency getUrgency() {
        return urgency;
    }

    public void setUrgency(AssignmentUrgency urgency) {
        this.urgency = firstNonNull(urgency, AssignmentUrgency.N);
    }

    public AssignmentType getType() {
        return type;
    }

    public void setType(AssignmentType type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = nullToEmpty(description);
    }

    public Municipality getMunicipality() {
        return municipality;
    }

    public void setMunicipality(Municipality municipality) {
        this.municipality = municipality;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = nullToEmpty(address);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add(PROP_ID, getId())
                .add(PROP_OPENED, opened)
                .add(PROP_CLOSED, closed)
                .add(PROP_TYPE, type)
                .toString();
    }
}
