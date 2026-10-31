package net.pkhapps.idispatch.server.report;

import net.pkhapps.idispatch.server.assignment.Assignment;
import net.pkhapps.idispatch.server.assignment.AssignmentUrgency;
import net.pkhapps.idispatch.server.assignmenttype.AssignmentTypeListItem;
import net.pkhapps.idispatch.server.municipality.MunicipalityListItem;
import net.pkhapps.idispatch.server.resource.Resource;
import org.jspecify.annotations.Nullable;

import java.io.Serializable;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

public class ReportDTO implements Serializable {

    private final Assignment assignment;
    private final @Nullable AssignmentTypeListItem type;
    private final @Nullable MunicipalityListItem municipality;

    private final List<ReportResourceDTO> resources;

    private final AssignmentReport report;

    public ReportDTO(Assignment assignment, @Nullable AssignmentTypeListItem type,
                     @Nullable MunicipalityListItem municipality, List<ReportResourceDTO> resources,
                     AssignmentReport report) {
        this.assignment = assignment;
        this.type = type;
        this.municipality = municipality;
        this.resources = Collections.unmodifiableList(resources);
        this.report = report;
    }

    /**
     * Returns a copy of this DTO with a different report, such as the one returned when the report was saved.
     */
    public ReportDTO withReport(AssignmentReport report) {
        return new ReportDTO(assignment, type, municipality, resources, report);
    }

    public Assignment getAssignment() {
        return assignment;
    }

    /**
     * The assignment's type, or {@code null} if it has none.
     */
    public @Nullable AssignmentTypeListItem getType() {
        return type;
    }

    /**
     * The assignment's municipality, or {@code null} if it has none.
     */
    public @Nullable MunicipalityListItem getMunicipality() {
        return municipality;
    }

    public Instant getOpened() {
        return assignment.opened();
    }

    public Instant getClosed() {
        return assignment.closed();
    }

    public AssignmentUrgency getUrgency() {
        return assignment.urgency();
    }

    public String getDescription() {
        return assignment.description();
    }

    public String getAddress() {
        return assignment.address();
    }

    public Long getNumber() {
        return assignment.id().value();
    }

    public List<ReportResourceDTO> getResources() {
        return resources;
    }

    /**
     * Returns every resource that responded to the assignment once, in call sign order.
     */
    public List<Resource> getRespondingResources() {
        return resources.stream().map(ReportResourceDTO::getResource).distinct().sorted().toList();
    }

    /**
     * Returns what happened on scene. Not yet saved if nobody has filled it in.
     */
    public AssignmentReport getReport() {
        return report;
    }
}
