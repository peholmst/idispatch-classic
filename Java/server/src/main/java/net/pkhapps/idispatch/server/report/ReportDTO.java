package net.pkhapps.idispatch.server.report;

import net.pkhapps.idispatch.server.assignment.Assignment;
import net.pkhapps.idispatch.server.assignment.AssignmentUrgency;
import net.pkhapps.idispatch.server.resource.Resource;

import java.io.Serializable;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

public class ReportDTO implements Serializable {

    private final Assignment assignment;

    private final List<ReportResourceDTO> resources;

    private final AssignmentReport report;

    public ReportDTO(Assignment assignment, List<ReportResourceDTO> resources, AssignmentReport report) {
        this.assignment = assignment;
        this.resources = Collections.unmodifiableList(resources);
        this.report = report;
    }

    /**
     * Returns a copy of this DTO with a different report, such as the one returned when the report was saved.
     */
    public ReportDTO withReport(AssignmentReport report) {
        return new ReportDTO(assignment, resources, report);
    }

    public Assignment getAssignment() {
        return assignment;
    }

    public Instant getOpened() {
        return assignment.getOpened();
    }

    public Instant getClosed() {
        return assignment.getClosed();
    }

    public AssignmentUrgency getUrgency() {
        return assignment.getUrgency();
    }

    public String getDescription() {
        return assignment.getDescription();
    }

    public String getAddress() {
        return assignment.getAddress();
    }

    public Long getNumber() {
        return assignment.getId();
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
