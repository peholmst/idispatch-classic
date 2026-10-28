package net.pkhapps.idispatch.server.boundary;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies that the {@code @Secured} annotations on the services are enforced.
 */
@SpringBootTest
class MethodSecurityTest {

    private static final DefaultFilter ALL = new DefaultFilter(true, null);

    @Autowired
    MunicipalityManagementService municipalityManagementService;
    @Autowired
    UserManagementService userManagementService;
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
        assertThatThrownBy(() -> userManagementService.findAll(ALL)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> reportService.findReportByAssignmentId(1L)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    void dispatcher_canUseDispatchServices() {
        assertThatCode(() -> assignmentService.findOpenAssignments()).doesNotThrowAnyException();
        assertThatCode(() -> resourceStatusService.getStatusOfAllResources()).doesNotThrowAnyException();
        assertThatCode(() -> assignmentService.findClosedAssignments()).doesNotThrowAnyException();
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
        assertThatCode(() -> assignmentService.findClosedAssignments()).doesNotThrowAnyException();
        assertThatCode(() -> reportService.findReportByAssignmentId(1L)).doesNotThrowAnyException();
        assertThatThrownBy(() -> assignmentService.findOpenAssignments()).isInstanceOf(AccessDeniedException.class);
    }
}
