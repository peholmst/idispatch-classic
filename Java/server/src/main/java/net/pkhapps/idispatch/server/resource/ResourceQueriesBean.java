package net.pkhapps.idispatch.server.resource;

import net.pkhapps.idispatch.server.assignment.AssignmentId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Service
@Transactional(readOnly = true)
class ResourceQueriesBean implements ResourceQueries {

    private final ResourceRepository resourceRepository;
    private final ResourceStatusRepository resourceStatusRepository;

    ResourceQueriesBean(ResourceRepository resourceRepository, ResourceStatusRepository resourceStatusRepository) {
        this.resourceRepository = resourceRepository;
        this.resourceStatusRepository = resourceStatusRepository;
    }

    @Override
    public List<ResourceListItem> findActive() {
        return resourceRepository.findListItems(false, null);
    }

    @Override
    public List<ResourceListItem> findByIds(Collection<ResourceId> ids) {
        return ids.isEmpty() ? List.of() : resourceRepository.findListItems(ids);
    }

    @Override
    public List<ResourceListItem> findByCallSignContaining(String searchTerm) {
        return resourceRepository.findListItems(true, searchTerm);
    }

    @Override
    public List<ArchivedResourceStatus> findPastStatesOn(AssignmentId assignment) {
        return resourceStatusRepository.findArchivedOn(assignment);
    }
}
