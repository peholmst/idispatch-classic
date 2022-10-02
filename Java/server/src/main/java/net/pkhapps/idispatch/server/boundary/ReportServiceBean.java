package net.pkhapps.idispatch.server.boundary;

import net.pkhapps.idispatch.server.entity.ArchivedResourceStatus;
import net.pkhapps.idispatch.server.entity.Assignment;
import net.pkhapps.idispatch.server.entity.Resource;
import net.pkhapps.idispatch.server.entity.repository.ArchivedResourceStatusRepository;
import net.pkhapps.idispatch.server.entity.repository.AssignmentRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
class ReportServiceBean implements ReportService {

    private final ArchivedResourceStatusRepository archivedResourceStatusRepository;

    private final AssignmentRepository assignmentRepository;

    ReportServiceBean(ArchivedResourceStatusRepository archivedResourceStatusRepository, AssignmentRepository assignmentRepository) {
        this.archivedResourceStatusRepository = archivedResourceStatusRepository;
        this.assignmentRepository = assignmentRepository;
    }

    @Override
    public Optional<ReportDTO> findReportByAssignmentId(long assignmentId) {
        Assignment assignment = assignmentRepository.findById(assignmentId).orElse(null);
        if (assignment == null || assignment.isAssignmentOpen()) {
            return Optional.empty();
        } else {
            List<ReportResourceDTO> reportResources = new ReportResourceGenerator(assignment).getReportResources();
            return Optional.of(new ReportDTO(assignment, reportResources));
        }
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
