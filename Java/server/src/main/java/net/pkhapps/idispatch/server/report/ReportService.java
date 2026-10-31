package net.pkhapps.idispatch.server.report;

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
import net.pkhapps.idispatch.server.resource.ResourceId;
import net.pkhapps.idispatch.server.resource.ResourceListItem;
import net.pkhapps.idispatch.server.resource.ResourceQueries;
import net.pkhapps.idispatch.server.security.Roles;
import net.pkhapps.idispatch.server.shared.AbstractService;
import net.pkhapps.idispatch.server.shared.UpdateResult;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * Lets report users find closed assignments and read their reports, writers fill in what happened on scene, and
 * reviewers close and re-open the reports.
 */
@Service
@PreAuthorize(Roles.HAS_ANY_REPORT_ROLE)
public class ReportService extends AbstractService {

    private final AssignmentQueries assignmentQueries;

    private final AssignmentReportRepository assignmentReportRepository;

    private final ClosedAssignmentQuery closedAssignmentQuery;

    private final MunicipalityQueries municipalityQueries;

    private final AssignmentTypeQueries assignmentTypeQueries;

    private final ResourceQueries resourceQueries;


    ReportService(ApplicationContext applicationContext, PlatformTransactionManager txManager, AssignmentQueries assignmentQueries, AssignmentReportRepository assignmentReportRepository, ClosedAssignmentQuery closedAssignmentQuery, MunicipalityQueries municipalityQueries, AssignmentTypeQueries assignmentTypeQueries, ResourceQueries resourceQueries) {
        super(applicationContext, txManager);
        this.assignmentQueries = assignmentQueries;
        this.assignmentReportRepository = assignmentReportRepository;
        this.closedAssignmentQuery = closedAssignmentQuery;
        this.municipalityQueries = municipalityQueries;
        this.assignmentTypeQueries = assignmentTypeQueries;
        this.resourceQueries = resourceQueries;
    }

    /**
     * Finds the report of the closed assignment with the specified ID. If nobody has filled in what happened on scene
     * yet, the report contains an empty, unsaved {@link AssignmentReport}.
     */
    public Optional<ReportDTO> findReportByAssignmentId(long assignmentId) {
        Assignment assignment = assignmentQueries.findById(new AssignmentId(assignmentId)).orElse(null);
        if (assignment == null || assignment.isOpen()) {
            return Optional.empty();
        } else {
            List<ReportResourceDTO> reportResources = new ReportResourceGenerator(assignment).getReportResources();
            AssignmentReport report = assignmentReportRepository.findByAssignment(assignment.id())
                    .orElseGet(() -> AssignmentReport.empty(assignment.id()));
            var type = assignment.typeId() == null ? null
                    : assignmentTypeQueries.findById(assignment.typeId()).orElse(null);
            var municipality = assignment.municipalityId() == null ? null
                    : municipalityQueries.findById(assignment.municipalityId()).orElse(null);
            return Optional.of(new ReportDTO(assignment, type, municipality, reportResources, report));
        }
    }

    /**
     * Finds one page of the closed assignments that match the filter, sorted by the page request by the {@code SORT_}
     * properties of {@link ClosedAssignmentListItem}. A report is open until a reviewer closes it, including when
     * nobody has written anything in it yet.
     */
    public List<ClosedAssignmentListItem> findClosedAssignments(ClosedAssignmentFilter filter, Pageable pageable) {
        logger.debug("Looking up closed assignments matching {}, {}", filter, pageable);
        return closedAssignmentQuery.find(filter, pageable);
    }

    /**
     * Counts the closed assignments that match the filter.
     */
    public int countClosedAssignments(ClosedAssignmentFilter filter) {
        return closedAssignmentQuery.count(filter);
    }

    /**
     * Returns the municipalities that at least one closed assignment is in, deleted or not, in no particular order.
     */
    public List<MunicipalityListItem> findMunicipalitiesOfClosedAssignments() {
        return municipalityQueries.findByIds(closedAssignmentQuery.findMunicipalityIds().stream()
                .map(MunicipalityId::new).toList());
    }

    /**
     * Returns the types that at least one closed assignment has, deleted or not, by code.
     */
    public List<AssignmentTypeListItem> findTypesOfClosedAssignments() {
        return assignmentTypeQueries.findByIds(closedAssignmentQuery.findTypeIds().stream()
                .map(AssignmentTypeId::new).toList());
    }

    /**
     * Saves what a writer filled in, recording the edit on the parts they changed, and returns:
     * <ul>
     * <li>{@link UpdateResult.Success} with the saved report if the operation was successful.</li>
     * <li>{@link UpdateResult.Conflict} with the other user's report if another user saved the report after the writer
     * read it.</li>
     * <li>{@link UpdateResult.NoChange} with the closed report if a reviewer has closed it; closed reports can't be
     * changed.</li>
     * </ul>
     *
     * @param seen the report as the writer read it, whose version must still be the stored one
     */
    @PreAuthorize(Roles.HAS_REPORT_WRITER)
    public UpdateResult<AssignmentReport> saveReport(AssignmentReport seen, ReportDraft draft) {
        logger.debug("Saving the report of assignment {} version {}", seen.assignmentId().value(), seen.version());
        final UserStamp edit = stampForCurrentUser();
        return changeReport(seen, true, current -> current.withDraft(draft, edit));
    }

    /**
     * Closes the report after a reviewer has checked it, so that nobody can change it. The report must be the version
     * the reviewer saw, so that nobody closes changes they haven't checked. Returns:
     * <ul>
     * <li>{@link UpdateResult.Success} with the closed report if the operation was successful.</li>
     * <li>{@link UpdateResult.Conflict} with the current report if it has changed since the reviewer read it.</li>
     * </ul>
     */
    @PreAuthorize(Roles.HAS_REPORT_REVIEWER)
    public UpdateResult<AssignmentReport> closeReport(AssignmentReport seen) {
        logger.debug("Closing the report of assignment {}", seen.assignmentId().value());
        final UserStamp closed = stampForCurrentUser();
        return changeReport(seen, false, current -> current.closedBy(closed));
    }

    /**
     * Re-opens a closed report, so that it can be changed again. Returns the same results as
     * {@link #closeReport(AssignmentReport)}.
     */
    @PreAuthorize(Roles.HAS_REPORT_REVIEWER)
    public UpdateResult<AssignmentReport> reopenReport(AssignmentReport seen) {
        logger.debug("Re-opening the report of assignment {}", seen.assignmentId().value());
        return changeReport(seen, false, AssignmentReport::reopened);
    }

    /**
     * Applies the change to the stored report, but only if it is still the version the user saw, so that nobody saves
     * over or reviews changes they haven't read. A report that nobody has written anything in counts as an empty one.
     *
     * @param refuseClosed whether a closed report is left as it is, which is {@link UpdateResult.NoChange} with the
     *                     stored report whichever version the user saw
     */
    private UpdateResult<AssignmentReport> changeReport(AssignmentReport seen, boolean refuseClosed,
                                                        UnaryOperator<AssignmentReport> change) {
        try {
            return getTxTemplate().execute(status -> {
                var stored = assignmentReportRepository.findByAssignment(seen.assignmentId());
                // Decided by what is stored, not by the copy the user had open, which may be from before it was closed
                if (refuseClosed && stored.filter(AssignmentReport::isClosed).isPresent()) {
                    logger.debug("The report of assignment {} is closed, cannot change it",
                            seen.assignmentId().value());
                    return new UpdateResult.NoChange<>(stored.get());
                }
                var unchanged = seen.isNew() ? stored.isEmpty()
                        : stored.filter(report -> Objects.equals(report.version(), seen.version())).isPresent();
                if (!unchanged) {
                    logger.debug("The report of assignment {} has changed since it was read",
                            seen.assignmentId().value());
                    return new UpdateResult.Conflict<>(seen, stored.orElse(null));
                }
                var current = stored.orElseGet(() -> AssignmentReport.empty(seen.assignmentId()));
                var changed = change.apply(current);
                return new UpdateResult.Success<>(current.isNew() ? assignmentReportRepository.insert(changed)
                        : assignmentReportRepository.update(changed));
            });
        } catch (OptimisticLockingFailureException | DataIntegrityViolationException ex) {
            // Either another user saved this report after we read it, or two users wrote the first report of the
            // same assignment at the same time and the other one won.
            logger.debug("The report of assignment {} was changed by another user at the same time",
                    seen.assignmentId().value());
            return new UpdateResult.Conflict<>(seen,
                    assignmentReportRepository.findByAssignment(seen.assignmentId()).orElse(null));
        }
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
            var statuses = resourceQueries.findPastStatesOn(assignment.id());
            resourceQueries.findByIds(statuses.stream().map(ArchivedResourceStatus::resourceId).distinct().toList())
                    .forEach(resource -> resources.put(resource.id(), resource));
            statuses.forEach(this::process);
            dtoList.addAll(builderMap.values().stream().map(ReportResourceDTO.Builder::build).toList());
        }

        public List<ReportResourceDTO> getReportResources() {
            return dtoList;
        }

        private void process(ArchivedResourceStatus status) {
            switch (status.state()) {
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
            ReportResourceDTO.Builder builder = getBuilder(status.resourceId());
            if (getter.get(builder) == null) {
                setter.set(builder, status.timestamp());
            } else {
                dtoList.add(builder.build());
                builderMap.remove(status.resourceId());
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
