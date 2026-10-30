package net.pkhapps.idispatch.server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

import static com.google.common.base.Strings.nullToEmpty;
import static net.pkhapps.idispatch.server.entity.AssignmentReport.MAX_TEXT_LENGTH;

/**
 * What one responding unit found and did on scene. Part of an {@link AssignmentReport}.
 */
@Embeddable
public class UnitReport implements Serializable {

    @Column(name = "situation_on_arrival", nullable = false, length = MAX_TEXT_LENGTH)
    @Size(max = MAX_TEXT_LENGTH, message = "Please describe the situation on arrival in at most 4000 characters")
    private String situationOnArrival = "";
    @Column(name = "actions_taken", nullable = false, length = MAX_TEXT_LENGTH)
    @Size(max = MAX_TEXT_LENGTH, message = "Please describe the actions taken in at most 4000 characters")
    private String actionsTaken = "";
    @Embedded
    private LastEdit lastEdit;

    public String getSituationOnArrival() {
        return situationOnArrival;
    }

    public void setSituationOnArrival(String situationOnArrival) {
        this.situationOnArrival = nullToEmpty(situationOnArrival);
    }

    public String getActionsTaken() {
        return actionsTaken;
    }

    public void setActionsTaken(String actionsTaken) {
        this.actionsTaken = nullToEmpty(actionsTaken);
    }

    /**
     * Returns who last changed this unit's report, or {@code null} if nobody has.
     */
    public LastEdit getLastEdit() {
        return lastEdit;
    }

    public void setLastEdit(LastEdit lastEdit) {
        this.lastEdit = lastEdit;
    }

    public boolean hasSameContentAs(UnitReport other) {
        return situationOnArrival.equals(other.situationOnArrival) && actionsTaken.equals(other.actionsTaken);
    }
}
