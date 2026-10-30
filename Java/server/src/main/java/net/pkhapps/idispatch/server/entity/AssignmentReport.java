package net.pkhapps.idispatch.server.entity;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

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
 * remember who last changed them; the report's {@link LastEdit} covers the situation and the counts only.
 */
@Entity
@Table(name = "assignment_reports")
public class AssignmentReport extends AbstractLockableEntity {

    public static final int MAX_TEXT_LENGTH = 4000;

    @OneToOne(optional = false)
    @JoinColumn(name = "assignment_id", nullable = false, unique = true)
    private Assignment assignment;
    @Column(name = "situation_development", nullable = false, length = MAX_TEXT_LENGTH)
    @Size(max = MAX_TEXT_LENGTH, message = "Please describe how the situation developed in at most 4000 characters")
    private String situationDevelopment = "";
    @Column(name = "people_involved")
    @Min(value = 0, message = "The number of people involved cannot be negative")
    private Integer peopleInvolved;
    @Column(name = "lightly_injured")
    @Min(value = 0, message = "The number of lightly injured cannot be negative")
    private Integer lightlyInjured;
    @Column(name = "seriously_injured")
    @Min(value = 0, message = "The number of seriously injured cannot be negative")
    private Integer seriouslyInjured;
    @Column(name = "deceased")
    @Min(value = 0, message = "The number of deceased cannot be negative")
    private Integer deceased;
    @Embedded
    private LastEdit lastEdit;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "assignment_report_units", joinColumns = @JoinColumn(name = "report_id"))
    @MapKeyJoinColumn(name = "resource_id")
    private Map<Resource, @Valid UnitReport> unitReports = new HashMap<>();

    protected AssignmentReport() {
    }

    public AssignmentReport(Assignment assignment) {
        this.assignment = checkNotNull(assignment);
    }

    public Assignment getAssignment() {
        return assignment;
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
    public LastEdit getLastEdit() {
        return lastEdit;
    }

    public void setLastEdit(LastEdit lastEdit) {
        this.lastEdit = lastEdit;
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

    @AssertTrue(message = "The injured and deceased cannot add up to more than the number of people involved")
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
                .add("assignment", assignment == null ? null : assignment.getId())
                .toString();
    }
}
