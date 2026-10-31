package net.pkhapps.idispatch.server.resource;


import net.pkhapps.idispatch.server.shared.management.ManagementService;

import java.util.List;
import java.util.Optional;

public interface ResourceManagementService extends ManagementService.SoftDeletable<Resource> {

    /**
     * Finds the types that a resource can be given, which are the ones not deleted, ordered by code.
     */
    List<ResourceTypeListItem> findApplicableTypes();

    /**
     * Finds the type with the given ID, whether deleted or not.
     */
    Optional<ResourceTypeListItem> findType(ResourceTypeId id);
}
