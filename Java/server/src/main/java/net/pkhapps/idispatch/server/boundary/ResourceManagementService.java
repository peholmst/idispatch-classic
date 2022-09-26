package net.pkhapps.idispatch.server.boundary;

import net.pkhapps.idispatch.server.entity.Resource;
import net.pkhapps.idispatch.server.entity.ResourceType;

import java.util.List;

public interface ResourceManagementService extends ManagementService.SoftDeletable<Resource> {

    List<ResourceType> findApplicableTypes();
}
