package net.pkhapps.idispatch.server.resource;


import net.pkhapps.idispatch.server.shared.management.ManagementService;

import java.util.List;

public interface ResourceManagementService extends ManagementService.SoftDeletable<Resource> {

    List<ResourceType> findApplicableTypes();
}
