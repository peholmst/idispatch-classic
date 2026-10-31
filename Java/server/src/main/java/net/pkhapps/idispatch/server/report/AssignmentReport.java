package net.pkhapps.idispatch.server.report;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import net.pkhapps.idispatch.server.assignment.AssignmentId;
import net.pkhapps.idispatch.server.resource.Resource;
import net.pkhapps.idispatch.server.shared.AbstractLockableEntity;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.nullToEmpty;

/**
 * Entity describing what happened on scene of a closed {@link Assignment}, as told by the people who were there. The
 * casualty counts are {@code null} until someone fills them in. The report as a whole and each {@link UnitReport}
 * remember who last changed them; the report's last edit covers the situation and the counts only.
 * <p>
 * Once a reviewer has checked the report, they close it, and nobody can change it until it is re-opened.
 */
@Entity
@Table(name = "assignment_reports")
public class AssignmentReport extends AbstractLockableEntity {

    public static final int MAX_TEXT_LENGTH = 4000;

    @Column(name = "assignment_id", nullable = false, unique = true)
    private Long assignmentId;
    @Column(name = "situation_development", nullable = false, length = MAX_TEXT_LENGTH)
    @Size(max = MAX_TEXT_LENGTH, message = "{idispatch.report.situation-development.size}")
    private String situationDevelopment = "";
    @Column(name = "people_involved")
    @Min(value = 0, message = "{idispatch.report.people-involved.min}")
    private Integer peopleInvolved;
    @Column(name = "lightly_injured")
    @Min(value = 0, message = "{idispatch.report.lightly-injured.min}")
    private Integer lightlyInjured;
    @Column(name = "seriously_injured")
    @Min(value = 0, message = "{idispatch.report.seriously-injured.min}")
    private Integer seriouslyInjured;
    @Column(name = "deceased")
    @Min(value = 0, message = "{idispatch.report.deceased.min}")
    private Integer deceased;
    @Embedded
    @AttributeOverride(name = "subject", column = @Column(name = "last_edited_by_subject"))
    @AttributeOverride(name = "firstName", column = @Column(name = "last_edited_by_first_name"))
    @AttributeOverride(name = "lastName", column = @Column(name = "last_edited_by_last_name"))
    @AttributeOverride(name = "timestamp", column = @Column(name = "last_edited_at"))
    private UserStamp lastEdit;
    @Embedded
    @AttributeOverride(name = "subject", column = @Column(name = "closed_by_subject"))
    @AttributeOverride(name = "firstName", column = @Column(name = "closed_by_first_name"))
    @AttributeOverride(name = "lastName", column = @Column(name = "closed_by_last_name"))
    @AttributeOverride(name = "timestamp", column = @Column(name = "closed_at"))
    private UserStamp closed;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "assignment_report_units", joinColumns = @JoinColumn(name = "report_id"))
    @MapKeyJoinColumn(name = "resource_id")
    private Map<Resource, @Valid UnitReport> unitReports = new HashMap<>();

    protected AssignmentReport() {
    }

    public AssignmentReport(AssignmentId assignment) {
        this.assignmentId = checkNotNull(assignment).value();
    }

    public AssignmentId getAssignmentId() {
        return new AssignmentId(assignmentId);
    }

    public String getSituationDevelopment() {
        return situationDevelopment;
    }

    public void setSituationDevelopment(String situationDevelopment) {
        this.situationDevelopment = nullToEmpty(situationDevelopment);
    }

    public Integer getPeopleInvolved() {
        return peopleInvolved;
    }

    public void setPeopleInvolved(Integer peopleInvolved) {
        this.peopleInvolved = peopleInvolved;
    }

    public Integer getLightlyInjured() {
        return lightlyInjured;
    }

    public void setLightlyInjured(Integer lightlyInjured) {
        this.lightlyInjured = lightlyInjured;
    }

    public Integer getSeriouslyInjured() {
        return seriouslyInjured;
    }

    public void setSeriouslyInjured(Integer seriouslyInjured) {
        this.seriouslyInjured = seriouslyInjured;
    }

    public Integer getDeceased() {
        return deceased;
    }

    public void setDeceased(Integer deceased) {
        this.deceased = deceased;
    }

    /**
     * Returns who last changed the situation or the counts, or {@code null} if nobody has.
     */
    public UserStamp getLastEdit() {
        return lastEdit;
    }

    public void setLastEdit(UserStamp lastEdit) {
        this.lastEdit = lastEdit;
    }

    /**
     * Returns who closed the report, or {@code null} if it is open.
     */
    public UserStamp getClosed() {
        return closed;
    }

    public boolean isClosed() {
        return closed != null;
    }

    public void close(UserStamp closed) {
        this.closed = checkNotNull(closed);
    }

    public void reopen() {
        this.closed = null;
    }

    /**
     * Returns whether this report has the same situation and counts as the other one, ignoring the unit reports.
     */
    public boolean hasSameSceneDetailsAs(AssignmentReport other) {
        return situationDevelopment.equals(other.situationDevelopment)
                && Objects.equals(peopleInvolved, other.peopleInvolved)
                && Objects.equals(lightlyInjured, other.lightlyInjured)
                && Objects.equals(seriouslyInjured, other.seriouslyInjured)
                && Objects.equals(deceased, other.deceased);
    }

    /**
     * Returns the reports of the units that have reported, or have been shown a form to report in.
     */
    public Map<Resource, UnitReport> getUnitReports() {
        return Collections.unmodifiableMap(unitReports);
    }

    /**
     * Returns the report of the given responding unit, creating an empty one if the unit has not reported yet.
     */
    public UnitReport getUnitReport(Resource resource) {
        return unitReports.computeIfAbsent(checkNotNull(resource), r -> new UnitReport());
    }

    @AssertTrue(message = "{idispatch.report.casualties.consistent}")
    public boolean isCasualtiesWithinPeopleInvolved() {
        if (peopleInvolved == null) {
            return true;
        }
        return zeroIfNull(lightlyInjured) + zeroIfNull(seriouslyInjured) + zeroIfNull(deceased) <= peopleInvolved;
    }

    private static int zeroIfNull(Integer value) {
        return value == null ? 0 : value;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add(PROP_ID, getId())
                .add("assignment", assignmentId)
                .toString();
    }
}
