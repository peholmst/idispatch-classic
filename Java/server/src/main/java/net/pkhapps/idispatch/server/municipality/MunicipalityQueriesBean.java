package net.pkhapps.idispatch.server.municipality;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
class MunicipalityQueriesBean implements MunicipalityQueries {

    private final MunicipalityRepository repository;

    MunicipalityQueriesBean(MunicipalityRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<MunicipalityListItem> findActive() {
        return repository.findListItems(false, null);
    }

    @Override
    public Optional<MunicipalityListItem> findById(MunicipalityId id) {
        return repository.findListItem(id);
    }

    @Override
    public List<MunicipalityListItem> findByIds(Collection<MunicipalityId> ids) {
        return ids.isEmpty() ? List.of() : repository.findListItems(ids);
    }
}
