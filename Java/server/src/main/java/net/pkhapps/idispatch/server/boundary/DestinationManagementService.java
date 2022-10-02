package net.pkhapps.idispatch.server.boundary;

import net.pkhapps.idispatch.server.entity.Destination;
import net.pkhapps.idispatch.server.entity.Resource;

import java.util.List;

public interface DestinationManagementService extends ManagementService.SoftDeletable<Destination> {

    List<Resource> findApplicableResources();
}
