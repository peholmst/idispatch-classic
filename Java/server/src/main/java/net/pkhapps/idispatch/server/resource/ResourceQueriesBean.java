package net.pkhapps.idispatch.server.resource;

import org.jooq.DSLContext;
import org.jooq.Record3;
import org.jooq.SelectJoinStep;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

import static net.pkhapps.idispatch.server.jooq.Tables.RESOURCES;

@Service
@Transactional(readOnly = true)
class ResourceQueriesBean implements ResourceQueries {

    private final DSLContext dsl;

    ResourceQueriesBean(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public List<ResourceListItem> findActive() {
        return selectListItems()
                .where(RESOURCES.ACTIVE.isTrue())
                .orderBy(RESOURCES.CALL_SIGN)
                .fetch(ResourceQueriesBean::toListItem);
    }

    @Override
    public List<ResourceListItem> findByIds(Collection<ResourceId> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return selectListItems()
                .where(RESOURCES.ID.in(ids.stream().map(ResourceId::value).toList()))
                .orderBy(RESOURCES.CALL_SIGN)
                .fetch(ResourceQueriesBean::toListItem);
    }

    private SelectJoinStep<Record3<Long, String, Boolean>> selectListItems() {
        return dsl.select(RESOURCES.ID, RESOURCES.CALL_SIGN, RESOURCES.ACTIVE).from(RESOURCES);
    }

    private static ResourceListItem toListItem(Record3<Long, String, Boolean> row) {
        return new ResourceListItem(new ResourceId(row.value1()), row.value2(), row.value3());
    }
}
