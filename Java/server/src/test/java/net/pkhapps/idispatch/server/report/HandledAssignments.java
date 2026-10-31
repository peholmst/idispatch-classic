package net.pkhapps.idispatch.server.report;

import net.pkhapps.idispatch.server.assignment.Assignment;
import net.pkhapps.idispatch.server.assignment.AssignmentService;
import net.pkhapps.idispatch.server.assignment.AssignmentUrgency;
import net.pkhapps.idispatch.server.assignmenttype.AssignmentTypeId;
import net.pkhapps.idispatch.server.dispatch.AssignmentNotReadyForDispatchException;
import net.pkhapps.idispatch.server.dispatch.DispatchService;
import net.pkhapps.idispatch.server.municipality.MunicipalityId;
import net.pkhapps.idispatch.server.resource.ResourceState;
import net.pkhapps.idispatch.server.resource.ResourceStatusService;
import net.pkhapps.idispatch.server.security.Roles;
import net.pkhapps.idispatch.server.shared.UpdateResult;
import net.pkhapps.idispatch.server.shared.ValidationFailedException;
import net.pkhapps.idispatch.server.ui.Formatters;
import net.pkhapps.idispatch.server.ui.RunAs;
import net.pkhapps.idispatch.server.ui.TestData;
import org.springframework.boot.test.context.TestComponent;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.function.Supplier;
import java.util.stream.Collectors;
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
            return String.valueOf(assignment.id().value());
        }

        public String opened() {
            return Formatters.formatDateTime(assignment.opened());
        }

        public String closed() {
            return Formatters.formatDateTime(assignment.closed());
        }

        public LocalDate closedOn() {
            return LocalDate.ofInstant(assignment.closed(), ZoneId.systemDefault());
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
        var givenType = given.assignmentType(code, "Building fire");
        var givenMunicipality = given.municipality(unique("Town "));
        var resourceIds = Stream.of(callSigns)
                .map(callSign -> given.resource(callSign, ResourceState.AT_STATION).id())
                .collect(Collectors.toSet());

        var assignment = asDispatcher(() -> {
            var resources = resourceStatusService.getAllResources().stream()
                    .filter(r -> resourceIds.contains(r.getId())).toList();
            var opened = assignmentService.findAssignment(assignmentService.openAssignment()).orElseThrow();
            var filledIn = opened.withTypeId(new AssignmentTypeId(givenType.id()))
                    .withMunicipalityId(new MunicipalityId(givenMunicipality.id()))
                    .withAddress("Report Road 7")
                    .withDescription("Smoke seen")
                    .withUrgency(AssignmentUrgency.B);
            var updated = assignmentService.updateAssignment(filledIn).getData().orElseThrow();
            for (var resource : resources) {
                resourceStatusService.setResourceAssignment(resource, updated.id());
            }
            try {
                dispatchService.dispatchAllResources(updated);
            } catch (AssignmentNotReadyForDispatchException ex) {
                throw new AssertionError("The assignment could not be dispatched", ex);
            }
            for (var resource : resources) {
                resourceStatusService.setResourceState(resource, ResourceState.EN_ROUTE);
                resourceStatusService.setResourceState(resource, ResourceState.ON_SCENE);
                resourceStatusService.setResourceState(resource, ResourceState.AVAILABLE);
            }
            if (!assignmentService.closeAssignment(updated.id())) {
                throw new AssertionError("The assignment could not be closed");
            }
            return assignmentService.findAssignment(updated.id()).orElseThrow();
        });
        return new Handled(assignment, code + " - Building fire", givenMunicipality.nameFi());
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
        return asDispatcher(() -> reportService.findReportByAssignmentId(handled.assignment().id().value()).orElseThrow()
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
        return RunAs.dispatcher(action);
    }

    private <T> T as(String role, Supplier<T> action) {
        return RunAs.role(role, action);
    }
}
