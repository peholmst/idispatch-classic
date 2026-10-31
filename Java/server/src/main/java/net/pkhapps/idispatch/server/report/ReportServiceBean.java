package net.pkhapps.idispatch.server.report;

import jakarta.validation.Validator;
import net.pkhapps.idispatch.server.assignment.Assignment;
import net.pkhapps.idispatch.server.assignment.AssignmentId;
import net.pkhapps.idispatch.server.assignment.AssignmentQueries;
import net.pkhapps.idispatch.server.assignmenttype.AssignmentTypeId;
import net.pkhapps.idispatch.server.assignmenttype.AssignmentTypeListItem;
import net.pkhapps.idispatch.server.assignmenttype.AssignmentTypeQueries;
import net.pkhapps.idispatch.server.municipality.MunicipalityId;
import net.pkhapps.idispatch.server.municipality.MunicipalityListItem;
import net.pkhapps.idispatch.server.municipality.MunicipalityQueries;
import net.pkhapps.idispatch.server.resource.ArchivedResourceStatus;
import net.pkhapps.idispatch.server.resource.ArchivedResourceStatusRepository;
import net.pkhapps.idispatch.server.resource.ResourceId;
import net.pkhapps.idispatch.server.resource.ResourceListItem;
import net.pkhapps.idispatch.server.resource.ResourceQueries;
import net.pkhapps.idispatch.server.security.Roles;
import net.pkhapps.idispatch.server.shared.AbstractServiceBean;
import net.pkhapps.idispatch.server.shared.UpdateResult;
import net.pkhapps.idispatch.server.shared.ValidationFailedException;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;

@Service
@Secured({Roles.ROLE_REPORT_READER, Roles.ROLE_REPORT_WRITER, Roles.ROLE_REPORT_REVIEWER})
class ReportServiceBean extends AbstractServiceBean implements ReportService {

    private final ArchivedResourceStatusRepository archivedResourceStatusRepository;

    private final AssignmentQueries assignmentQueries;

    private final AssignmentReportRepository assignmentReportRepository;

    private final ClosedAssignmentQuery closedAssignmentQuery;

    private final MunicipalityQueries municipalityQueries;

    private final AssignmentTypeQueries assignmentTypeQueries;

    private final ResourceQueries resourceQueries;

    private final Validator validator;

    ReportServiceBean(ApplicationContext applicationContext, PlatformTransactionManager txManager, ArchivedResourceStatusRepository archivedResourceStatusRepository, AssignmentQueries assignmentQueries, AssignmentReportRepository assignmentReportRepository, ClosedAssignmentQuery closedAssignmentQuery, MunicipalityQueries municipalityQueries, AssignmentTypeQueries assignmentTypeQueries, ResourceQueries resourceQueries, Validator validator) {
        super(applicationContext, txManager);
        this.archivedResourceStatusRepository = archivedResourceStatusRepository;
        this.assignmentQueries = assignmentQueries;
        this.assignmentReportRepository = assignmentReportRepository;
        this.closedAssignmentQuery = closedAssignmentQuery;
        this.municipalityQueries = municipalityQueries;
        this.assignmentTypeQueries = assignmentTypeQueries;
        this.resourceQueries = resourceQueries;
        this.validator = validator;
    }

    @Override
    public Optional<ReportDTO> findReportByAssignmentId(long assignmentId) {
        Assignment assignment = assignmentQueries.findById(new AssignmentId(assignmentId)).orElse(null);
        if (assignment == null || assignment.isOpen()) {
            return Optional.empty();
        } else {
            List<ReportResourceDTO> reportResources = new ReportResourceGenerator(assignment).getReportResources();
            AssignmentReport report = assignmentReportRepository.findByAssignmentId(assignment.id().value())
                    .orElseGet(() -> new AssignmentReport(assignment.id()));
            var type = assignment.typeId() == null ? null
                    : assignmentTypeQueries.findById(assignment.typeId()).orElse(null);
            var municipality = assignment.municipalityId() == null ? null
                    : municipalityQueries.findById(assignment.municipalityId()).orElse(null);
            return Optional.of(new ReportDTO(assignment, type, municipality, reportResources, report));
        }
    }

    @Override
    public List<ClosedAssignmentListItem> findClosedAssignments(ClosedAssignmentFilter filter, Pageable pageable) {
        logger.debug("Looking up closed assignments matching {}, {}", filter, pageable);
        return closedAssignmentQuery.find(filter, pageable);
    }

    @Override
    public int countClosedAssignments(ClosedAssignmentFilter filter) {
        return closedAssignmentQuery.count(filter);
    }

    @Override
    public List<MunicipalityListItem> findMunicipalitiesOfClosedAssignments() {
        return municipalityQueries.findByIds(closedAssignmentQuery.findMunicipalityIds().stream()
                .map(MunicipalityId::new).toList());
    }

    @Override
    public List<AssignmentTypeListItem> findTypesOfClosedAssignments() {
        return assignmentTypeQueries.findByIds(closedAssignmentQuery.findTypeIds().stream()
                .map(AssignmentTypeId::new).toList());
    }

    @Override
    @Secured(Roles.ROLE_REPORT_WRITER)
    public UpdateResult<AssignmentReport> saveReport(AssignmentReport report) throws ValidationFailedException {
        logger.debug("Saving report {}", report);
        ValidationFailedException.throwIfNotEmpty(validator.validate(report));
        try {
            final UserStamp edit = stampForCurrentUser();
            return getTxTemplate().execute(status -> {
                var previous = report.isNew() ? null : assignmentReportRepository.findById(report.getId()).orElse(null);
                // Decided by what is stored, not by the copy the user had open, which may be from before it was closed
                if (previous != null && previous.isClosed()) {
                    logger.debug("Report {} is closed, cannot save", report);
                    return new UpdateResult.NoChange<>(previous);
                }
                stampChangedParts(report, previous, edit);
                return new UpdateResult.Success<>(assignmentReportRepository.saveAndFlush(report));
            });
        } catch (OptimisticLockingFailureException | DataIntegrityViolationException ex) {
            // Either another user saved this report after we read it, or two users wrote the first report of the
            // same assignment at the same time and the other one won.
            logger.debug("Report {} was saved by another user, cannot save", report);
            final AssignmentReport otherReport = assignmentReportRepository.findByAssignmentId(report.getAssignmentId().value()).orElse(null);
            return new UpdateResult.Conflict<>(report, otherReport);
        }
    }

    @Override
    @Secured(Roles.ROLE_REPORT_REVIEWER)
    public UpdateResult<AssignmentReport> closeReport(AssignmentReport report) {
        logger.debug("Closing report {}", report);
        final UserStamp closed = stampForCurrentUser();
        return changeReviewedReport(report, current -> current.close(closed));
    }

    @Override
    @Secured(Roles.ROLE_REPORT_REVIEWER)
    public UpdateResult<AssignmentReport> reopenReport(AssignmentReport report) {
        logger.debug("Re-opening report {}", report);
        return changeReviewedReport(report, AssignmentReport::reopen);
    }

    /**
     * Applies the change to the stored report, but only if it is still the version the reviewer saw. Works on the stored
     * report rather than the reviewer's copy, so that nothing else the reviewer's copy might hold gets saved.
     */
    private UpdateResult<AssignmentReport> changeReviewedReport(AssignmentReport seen, Consumer<AssignmentReport> change) {
        try {
            return getTxTemplate().execute(status -> {
                var current = assignmentReportRepository.findByAssignmentId(seen.getAssignmentId().value());
                var unchanged = seen.isNew() ? current.isEmpty()
                        : current.filter(stored -> stored.getVersion().equals(seen.getVersion())).isPresent();
                if (!unchanged) {
                    logger.debug("Report {} has changed since the reviewer read it", seen);
                    return new UpdateResult.Conflict<>(seen, current.orElse(null));
                }
                // A reviewer may close a report that nobody has written anything in
                var report = current.orElseGet(() -> new AssignmentReport(seen.getAssignmentId()));
                change.accept(report);
                return new UpdateResult.Success<>(assignmentReportRepository.saveAndFlush(report));
            });
        } catch (OptimisticLockingFailureException | DataIntegrityViolationException ex) {
            logger.debug("Report {} was changed by another user at the same time", seen);
            return new UpdateResult.Conflict<>(seen, assignmentReportRepository.findByAssignmentId(seen.getAssignmentId().value()).orElse(null));
        }
    }

    /**
     * Records the edit on the parts of the report whose content differs from the previously saved report, so that saving
     * a report doesn't claim the parts the user didn't touch. A part of a report that has not been saved before counts
     * as changed if it has any content.
     */
    private void stampChangedParts(AssignmentReport report, AssignmentReport previous, UserStamp edit) {
        var before = previous == null ? new AssignmentReport(report.getAssignmentId()) : previous;
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

    private UserStamp stampForCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication.getPrincipal() instanceof OidcUser user) {
            return new UserStamp(user.getSubject(), user.getGivenName(), user.getFamilyName(), Instant.now());
        }
        // Only when not logged in through Keycloak, such as in tests
        return new UserStamp(authentication.getName(), null, null, Instant.now());
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

        private final Map<ResourceId, ReportResourceDTO.Builder> builderMap = new HashMap<>();
        private final Map<ResourceId, ResourceListItem> resources = new HashMap<>();
        private final List<ReportResourceDTO> dtoList = new ArrayList<>();

        private ReportResourceGenerator(Assignment assignment) {
            this.assignment = assignment;
            process();
        }

        private void process() {
            var statuses = archivedResourceStatusRepository.findByAssignmentId(assignment.id().value());
            resourceQueries.findByIds(statuses.stream().map(ArchivedResourceStatus::getResourceId).distinct().toList())
                    .forEach(resource -> resources.put(resource.id(), resource));
            statuses.forEach(this::process);
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
            ReportResourceDTO.Builder builder = getBuilder(status.getResourceId());
            if (getter.get(builder) == null) {
                setter.set(builder, status.getTimestamp());
            } else {
                dtoList.add(builder.build());
                builderMap.remove(status.getResourceId());
                process(status, getter, setter);
            }
        }

        private ReportResourceDTO.Builder getBuilder(ResourceId resource) {
            ReportResourceDTO.Builder builder = builderMap.get(resource);
            if (builder == null) {
                builder = new ReportResourceDTO.Builder(resources.get(resource));
                builderMap.put(resource, builder);
            }
            return builder;
        }


    }

}
