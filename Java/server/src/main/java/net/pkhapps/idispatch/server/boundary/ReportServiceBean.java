package net.pkhapps.idispatch.server.boundary;

import jakarta.validation.Validator;
import net.pkhapps.idispatch.server.entity.ArchivedResourceStatus;
import net.pkhapps.idispatch.server.entity.Assignment;
import net.pkhapps.idispatch.server.entity.AssignmentReport;
import net.pkhapps.idispatch.server.entity.LastEdit;
import net.pkhapps.idispatch.server.entity.Resource;
import net.pkhapps.idispatch.server.entity.UnitReport;
import net.pkhapps.idispatch.server.entity.ValidationFailedException;
import net.pkhapps.idispatch.server.entity.repository.ArchivedResourceStatusRepository;
import net.pkhapps.idispatch.server.entity.repository.AssignmentReportRepository;
import net.pkhapps.idispatch.server.entity.repository.AssignmentRepository;
import net.pkhapps.idispatch.server.security.Roles;
import net.pkhapps.idispatch.server.util.UpdateResult;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Instant;
import java.util.*;

@Service
@Secured({Roles.ROLE_REPORT_READER, Roles.ROLE_REPORT_WRITER})
class ReportServiceBean extends AbstractServiceBean implements ReportService {

    private final ArchivedResourceStatusRepository archivedResourceStatusRepository;

    private final AssignmentRepository assignmentRepository;

    private final AssignmentReportRepository assignmentReportRepository;

    private final Validator validator;

    ReportServiceBean(ApplicationContext applicationContext, PlatformTransactionManager txManager, ArchivedResourceStatusRepository archivedResourceStatusRepository, AssignmentRepository assignmentRepository, AssignmentReportRepository assignmentReportRepository, Validator validator) {
        super(applicationContext, txManager);
        this.archivedResourceStatusRepository = archivedResourceStatusRepository;
        this.assignmentRepository = assignmentRepository;
        this.assignmentReportRepository = assignmentReportRepository;
        this.validator = validator;
    }

    @Override
    public Optional<ReportDTO> findReportByAssignmentId(long assignmentId) {
        Assignment assignment = assignmentRepository.findById(assignmentId).orElse(null);
        if (assignment == null || assignment.isAssignmentOpen()) {
            return Optional.empty();
        } else {
            List<ReportResourceDTO> reportResources = new ReportResourceGenerator(assignment).getReportResources();
            AssignmentReport report = assignmentReportRepository.findByAssignment(assignment)
                    .orElseGet(() -> new AssignmentReport(assignment));
            return Optional.of(new ReportDTO(assignment, reportResources, report));
        }
    }

    @Override
    @Secured(Roles.ROLE_REPORT_WRITER)
    public UpdateResult<AssignmentReport> saveReport(AssignmentReport report) throws ValidationFailedException {
        logger.debug("Saving report {}", report);
        ValidationFailedException.throwIfNotEmpty(validator.validate(report));
        try {
            final LastEdit edit = lastEditByCurrentUser();
            final AssignmentReport savedReport = getTxTemplate().execute(status -> {
                var previous = report.isNew() ? null : assignmentReportRepository.findById(report.getId()).orElse(null);
                stampChangedParts(report, previous, edit);
                return assignmentReportRepository.saveAndFlush(report);
            });
            return new UpdateResult.Success<>(savedReport);
        } catch (OptimisticLockingFailureException | DataIntegrityViolationException ex) {
            // Either another user saved this report after we read it, or two users wrote the first report of the
            // same assignment at the same time and the other one won.
            logger.debug("Report {} was saved by another user, cannot save", report);
            final AssignmentReport otherReport = assignmentReportRepository.findByAssignment(report.getAssignment()).orElse(null);
            return new UpdateResult.Conflict<>(report, otherReport);
        }
    }

    /**
     * Records the edit on the parts of the report whose content differs from the previously saved report, so that saving
     * a report doesn't claim the parts the user didn't touch. A part of a report that has not been saved before counts
     * as changed if it has any content.
     */
    private void stampChangedParts(AssignmentReport report, AssignmentReport previous, LastEdit edit) {
        var before = previous == null ? new AssignmentReport(report.getAssignment()) : previous;
        if (!report.hasSameSceneDetailsAs(before)) {
            report.setLastEdit(edit);
        }
        report.getUnitReports().forEach((resource, unitReport) -> {
            var unitBefore = before.getUnitReports().getOrDefault(resource, new UnitReport());
            if (!unitReport.hasSameContentAs(unitBefore)) {
                unitReport.setLastEdit(edit);
            }
        });
    }

    private LastEdit lastEditByCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication.getPrincipal() instanceof OidcUser user) {
            return new LastEdit(user.getSubject(), user.getGivenName(), user.getFamilyName(), Instant.now());
        }
        // Only when not logged in through Keycloak, such as in tests
        return new LastEdit(authentication.getName(), null, null, Instant.now());
    }

    @FunctionalInterface
    interface DateSetter {
        void set(ReportResourceDTO.Builder owner, Instant date);
    }

    @FunctionalInterface
    interface DateGetter {
        Instant get(ReportResourceDTO.Builder owner);
    }

    private class ReportResourceGenerator {

        private final Assignment assignment;

        private final Map<Resource, ReportResourceDTO.Builder> builderMap = new HashMap<>();
        private final List<ReportResourceDTO> dtoList = new ArrayList<>();

        private ReportResourceGenerator(Assignment assignment) {
            this.assignment = assignment;
            process();
        }

        private void process() {
            archivedResourceStatusRepository.findByAssignment(assignment).forEach(this::process);
            dtoList.addAll(builderMap.values().stream().map(ReportResourceDTO.Builder::build).toList());
        }

        public List<ReportResourceDTO> getReportResources() {
            return dtoList;
        }

        private void process(ArchivedResourceStatus status) {
            switch (status.getState()) {
                case AT_STATION:
                    process(status, ReportResourceDTO.Builder::getAtStation, ReportResourceDTO.Builder::setAtStation);
                    break;
                case AVAILABLE:
                    process(status, ReportResourceDTO.Builder::getAvailable, ReportResourceDTO.Builder::setAvailable);
                    break;
                case DISPATCHED:
                    process(status, ReportResourceDTO.Builder::getDispatched, ReportResourceDTO.Builder::setDispatched);
                    break;
                case EN_ROUTE:
                    process(status, ReportResourceDTO.Builder::getEnRoute, ReportResourceDTO.Builder::setEnRoute);
                    break;
                case ON_SCENE:
                    process(status, ReportResourceDTO.Builder::getOnScene, ReportResourceDTO.Builder::setOnScene);
            }
        }

        private void process(ArchivedResourceStatus status, DateGetter getter, DateSetter setter) {
            ReportResourceDTO.Builder builder = getBuilder(status.getResource());
            if (getter.get(builder) == null) {
                setter.set(builder, status.getTimestamp());
            } else {
                dtoList.add(builder.build());
                builderMap.remove(status.getResource());
                process(status, getter, setter);
            }
        }

        private ReportResourceDTO.Builder getBuilder(Resource resource) {
            ReportResourceDTO.Builder builder = builderMap.get(resource);
            if (builder == null) {
                builder = new ReportResourceDTO.Builder(resource);
                builderMap.put(resource, builder);
            }
            return builder;
        }


    }

}
