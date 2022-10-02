package net.pkhapps.idispatch.server.boundary;

import net.pkhapps.idispatch.server.entity.Assignment;
import net.pkhapps.idispatch.server.entity.AssignmentType;
import net.pkhapps.idispatch.server.entity.AssignmentUrgency;
import net.pkhapps.idispatch.server.entity.Municipality;

import java.io.Serializable;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

public class ReportDTO implements Serializable {

    private final Assignment assignment;

    private final List<ReportResourceDTO> resources;

    public ReportDTO(Assignment assignment, List<ReportResourceDTO> resources) {
        this.assignment = assignment;
        this.resources = Collections.unmodifiableList(resources);
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

    public AssignmentType getType() {
        return assignment.getType();
    }

    public String getDescription() {
        return assignment.getDescription();
    }

    public Municipality getMunicipality() {
        return assignment.getMunicipality();
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
}
