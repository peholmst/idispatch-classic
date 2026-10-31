package net.pkhapps.idispatch.server.security;

import net.pkhapps.idispatch.server.TestSecurityConfiguration;
import net.pkhapps.idispatch.server.TestcontainersConfiguration;
import net.pkhapps.idispatch.server.assignment.AssignmentService;
import net.pkhapps.idispatch.server.municipality.MunicipalityManagementService;
import net.pkhapps.idispatch.server.report.ClosedAssignmentFilter;
import net.pkhapps.idispatch.server.report.ReportService;
import net.pkhapps.idispatch.server.resource.ResourceStatusService;
import net.pkhapps.idispatch.server.shared.management.DefaultFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies that the {@code @PreAuthorize} annotations on the services are enforced.
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, TestSecurityConfiguration.class})
class MethodSecurityTest {

    private static final DefaultFilter ALL = new DefaultFilter(true, null);
    private static final ClosedAssignmentFilter NO_FILTER = new ClosedAssignmentFilter(null, null, null, null, null);
    private static final PageRequest FIRST_PAGE = PageRequest.of(0, 10);

    @Autowired
    MunicipalityManagementService municipalityManagementService;
    @Autowired
    AssignmentService assignmentService;
    @Autowired
    ResourceStatusService resourceStatusService;
    @Autowired
    ReportService reportService;

    @Test
    @WithMockUser(roles = "DISPATCHER")
    void dispatcher_cannotUseManagementOrReportServices() {
        assertThatThrownBy(() -> municipalityManagementService.findAll(ALL)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> reportService.findReportByAssignmentId(1L)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> reportService.findClosedAssignments(NO_FILTER, FIRST_PAGE)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> reportService.saveReport(null, null)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    void dispatcher_canUseDispatchServices() {
        assertThatCode(() -> assignmentService.findOpenAssignments()).doesNotThrowAnyException();
        assertThatCode(() -> resourceStatusService.getStatusOfAllResources()).doesNotThrowAnyException();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_cannotUseDispatchOrReportServices() {
        assertThatCode(() -> municipalityManagementService.findAll(ALL)).doesNotThrowAnyException();
        assertThatThrownBy(() -> assignmentService.findOpenAssignments()).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> resourceStatusService.getStatusOfAllResources()).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> reportService.findReportByAssignmentId(1L)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(roles = "REPORT_READER")
    void reportReader_canReadClosedAssignmentsAndReportsOnly() {
        assertThatCode(() -> reportService.findClosedAssignments(NO_FILTER, FIRST_PAGE)).doesNotThrowAnyException();
        assertThatCode(() -> reportService.findReportByAssignmentId(1L)).doesNotThrowAnyException();
        assertThatThrownBy(() -> assignmentService.findOpenAssignments()).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> reportService.saveReport(null, null)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> reportService.closeReport(null)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> reportService.reopenReport(null)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(roles = "REPORT_REVIEWER")
    void reportReviewer_canReadReportsButNotWriteThem() {
        assertThatCode(() -> reportService.findClosedAssignments(NO_FILTER, FIRST_PAGE)).doesNotThrowAnyException();
        assertThatCode(() -> reportService.findReportByAssignmentId(1L)).doesNotThrowAnyException();
        assertThatThrownBy(() -> reportService.saveReport(null, null)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> assignmentService.findOpenAssignments()).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(roles = "REPORT_WRITER")
    void reportWriter_canReadClosedAssignmentsAndReports() {
        assertThatCode(() -> reportService.findClosedAssignments(NO_FILTER, FIRST_PAGE)).doesNotThrowAnyException();
        assertThatCode(() -> reportService.findReportByAssignmentId(1L)).doesNotThrowAnyException();
        assertThatThrownBy(() -> reportService.closeReport(null)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> reportService.reopenReport(null)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> assignmentService.findOpenAssignments()).isInstanceOf(AccessDeniedException.class);
    }
}
