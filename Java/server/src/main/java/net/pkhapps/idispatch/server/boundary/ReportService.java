package net.pkhapps.idispatch.server.boundary;

import java.util.Optional;

public interface ReportService {

    Optional<ReportDTO> findReportByAssignmentId(long assignmentId);

}
