package net.pkhapps.idispatch.server.dispatch;

import net.pkhapps.idispatch.server.resource.Resource;
import net.pkhapps.idispatch.server.shared.management.ManagementService;

import java.util.List;

public interface DestinationManagementService extends ManagementService.SoftDeletable<Destination> {

    List<Resource> findApplicableResources();
}
