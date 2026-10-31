package net.pkhapps.idispatch.server.ui.reports;

import net.pkhapps.idispatch.server.boundary.AssignmentService;
import net.pkhapps.idispatch.server.boundary.DispatchService;
import net.pkhapps.idispatch.server.boundary.ReportService;
import net.pkhapps.idispatch.server.boundary.ResourceStatusService;
import net.pkhapps.idispatch.server.entity.Assignment;
import net.pkhapps.idispatch.server.entity.AssignmentReport;
import net.pkhapps.idispatch.server.entity.AssignmentUrgency;
import net.pkhapps.idispatch.server.entity.ResourceState;
import net.pkhapps.idispatch.server.entity.ValidationFailedException;
import net.pkhapps.idispatch.server.security.Roles;
import net.pkhapps.idispatch.server.ui.Formatters;
import net.pkhapps.idispatch.server.ui.TestData;
import net.pkhapps.idispatch.server.util.UpdateResult;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static net.pkhapps.idispatch.server.ui.TestData.unique;

/**
 * Sets up the starting situation of the report stories: assignments that dispatchers have handled from start to
 * finish and closed, each with a report waiting to be written. Works through the services, as a dispatcher in another
 * session would, so that the reports are exactly what the dispatching produces.
 */
@TestComponent
public class HandledAssignments {

    /**
     * What a closed assignment looks like in the reports.
     */
    public record Handled(Assignment assignment, String type, String municipality) {

        public String number() {
            return assignment.getId().toString();
        }

        public String opened() {
            return Formatters.formatDateTime(assignment.getOpened());
        }

        public String closed() {
            return Formatters.formatDateTime(assignment.getClosed());
        }

        public LocalDate closedOn() {
            return LocalDate.ofInstant(assignment.getClosed(), ZoneId.systemDefault());
        }
    }

    private final TestData given;
    private final AssignmentService assignmentService;
    private final ResourceStatusService resourceStatusService;
    private final DispatchService dispatchService;
    private final ReportService reportService;

    HandledAssignments(TestData given, AssignmentService assignmentService, ResourceStatusService resourceStatusService,
                       DispatchService dispatchService, ReportService reportService) {
        this.given = given;
        this.assignmentService = assignmentService;
        this.resourceStatusService = resourceStatusService;
        this.dispatchService = dispatchService;
        this.reportService = reportService;
    }

    /**
     * An urgency B building fire at Report Road 7 in a municipality and of a type of its own, to which a resource with
     * each of the given call signs was dispatched, drove, arrived and became available again before the assignment
     * was closed.
     */
    public Handled closedAssignment(String... callSigns) {
        var code = unique("T");
        var type = given.assignmentType(code, "Building fire");
        var municipality = given.municipality(unique("Town "));
        var resources = Stream.of(callSigns).map(callSign -> given.resource(callSign, ResourceState.AT_STATION)).toList();

        var assignment = asDispatcher(() -> {
            var opened = assignmentService.findAssignment(assignmentService.openAssignment()).orElseThrow();
            opened.setType(type);
            opened.setMunicipality(municipality);
            opened.setAddress("Report Road 7");
            opened.setDescription("Smoke seen");
            opened.setUrgency(AssignmentUrgency.B);
            var updated = assignmentService.updateAssignment(opened).getData().orElseThrow();
            for (var resource : resources) {
                resourceStatusService.setResourceAssignment(resource, updated);
            }
            try {
                dispatchService.dispatchAllResources(updated);
            } catch (ValidationFailedException ex) {
                throw new AssertionError("The assignment could not be dispatched", ex);
            }
            for (var resource : resources) {
                resourceStatusService.setResourceState(resource, ResourceState.EN_ROUTE);
                resourceStatusService.setResourceState(resource, ResourceState.ON_SCENE);
                resourceStatusService.setResourceState(resource, ResourceState.AVAILABLE);
            }
            if (!assignmentService.closeAssignment(updated)) {
                throw new AssertionError("The assignment could not be closed");
            }
            return assignmentService.findAssignment(updated.getId()).orElseThrow();
        });
        return new Handled(assignment, type.getFormattedDescription(Locale.ENGLISH), municipality.getNameFi());
    }

    /**
     * An assignment that a dispatcher has opened and is still working on.
     */
    public String openAssignment() {
        return asDispatcher(() -> assignmentService.openAssignment().toString());
    }

    /**
     * The report as it is stored.
     */
    public AssignmentReport reportOf(Handled handled) {
        return asDispatcher(() -> reportService.findReportByAssignmentId(handled.assignment().getId()).orElseThrow()
                .getReport());
    }

    /**
     * Closes the report, as a reviewer in another session does.
     */
    public void reviewerHasClosedTheReportOf(Handled handled) {
        var report = reportOf(handled);
        as(Roles.ROLE_REPORT_REVIEWER, () -> reportService.closeReport(report));
    }

    /**
     * Saves how the situation developed in the report, as a writer in another session does.
     */
    public void writerHasWrittenInTheReportOf(Handled handled, String situationDevelopment) {
        var report = reportOf(handled);
        report.setSituationDevelopment(situationDevelopment);
        as(Roles.ROLE_REPORT_WRITER, () -> {
            try {
                if (!(reportService.saveReport(report) instanceof UpdateResult.Success)) {
                    throw new AssertionError("The report could not be saved");
                }
            } catch (ValidationFailedException ex) {
                throw new AssertionError(ex);
            }
            return null;
        });
    }

    private <T> T asDispatcher(Supplier<T> action) {
        return as(Roles.ROLE_DISPATCHER, action);
    }

    private <T> T as(String role, Supplier<T> action) {
        var previous = SecurityContextHolder.getContext();
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated("fixture", null,
                AuthorityUtils.createAuthorityList(role, Roles.ROLE_REPORT_READER)));
        SecurityContextHolder.setContext(context);
        try {
            return action.get();
        } finally {
            SecurityContextHolder.setContext(previous);
        }
    }
}
