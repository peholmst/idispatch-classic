package net.pkhapps.idispatch.server.assignment;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import net.pkhapps.idispatch.server.assignmenttype.AssignmentTypeId;
import net.pkhapps.idispatch.server.municipality.MunicipalityId;
import net.pkhapps.idispatch.server.shared.AbstractLockableEntity;
import net.pkhapps.idispatch.server.shared.Bilingual;
import org.hibernate.annotations.Formula;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.Locale;

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
    public static final String PROP_TYPE_ID = "typeId";
    public static final String PROP_DESCRIPTION = "description";
    public static final String PROP_ADDRESS = "address";

    @Column(name = "opened", nullable = false)
    private Instant opened;
    @Column(name = "closed")
    private Instant closed;
    @Enumerated(EnumType.STRING)
    @Column(name = "urgency", nullable = false)
    private AssignmentUrgency urgency = AssignmentUrgency.N;
    @Column(name = "type_id")
    @NotNull(message = "{idispatch.assignment.type.required}", groups = DispatchValidationGroup.class)
    private Long typeId;
    @Column(name = "description", nullable = false)
    private String description = "";
    @Column(name = "municipality_id")
    @NotNull(message = "{idispatch.assignment.municipality.required}", groups = DispatchValidationGroup.class)
    private Long municipalityId;
    @Column(name = "address", nullable = false)
    @NotBlank(message = "{idispatch.assignment.address.required}", groups = DispatchValidationGroup.class)
    private String address = "";
    // Read-only, for showing the type and the municipality until assignments are stored with jOOQ too. They are read
    // when the assignment is loaded, so they are only up to date in an assignment loaded after its last change.
    @Formula("(select t.code from assignment_types t where t.id = type_id)")
    private String typeCode;
    @Formula("(select t.description_fi from assignment_types t where t.id = type_id)")
    private String typeDescriptionFi;
    @Formula("(select t.description_sv from assignment_types t where t.id = type_id)")
    private String typeDescriptionSv;
    @Formula("(select m.name_fi from municipalities m where m.id = municipality_id)")
    private String municipalityNameFi;
    @Formula("(select m.name_sv from municipalities m where m.id = municipality_id)")
    private String municipalityNameSv;

    public Assignment() {
        opened = Instant.now();
    }

    /**
     * Returns the ID by which other features refer to this assignment. Only for an assignment that has been saved.
     */
    public AssignmentId getAssignmentId() {
        return new AssignmentId(checkNotNull(getId(), "The assignment has not been saved"));
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

    public @Nullable AssignmentTypeId getTypeId() {
        return typeId == null ? null : new AssignmentTypeId(typeId);
    }

    public void setTypeId(@Nullable AssignmentTypeId typeId) {
        this.typeId = typeId == null ? null : typeId.value();
    }

    /**
     * Returns the code of the assignment's type as it was when the assignment was loaded, or {@code null} if it has no
     * type.
     */
    public @Nullable String getTypeCode() {
        return typeCode;
    }

    /**
     * Returns the description of the assignment's type to show to a user with the given locale, as it was when the
     * assignment was loaded, or {@code null} if it has no type.
     */
    public @Nullable String getTypeDescription(@Nullable Locale locale) {
        return typeCode == null ? null : Bilingual.select(typeDescriptionFi, typeDescriptionSv, locale);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = nullToEmpty(description);
    }

    public @Nullable MunicipalityId getMunicipalityId() {
        return municipalityId == null ? null : new MunicipalityId(municipalityId);
    }

    public void setMunicipalityId(@Nullable MunicipalityId municipalityId) {
        this.municipalityId = municipalityId == null ? null : municipalityId.value();
    }

    /**
     * Returns the name of the assignment's municipality to show to a user with the given locale, as it was when the
     * assignment was loaded, or {@code null} if it has no municipality.
     */
    public @Nullable String getMunicipalityName(@Nullable Locale locale) {
        return municipalityId == null ? null : Bilingual.select(municipalityNameFi, municipalityNameSv, locale);
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
                .add(PROP_TYPE_ID, typeId)
                .toString();
    }
}
