package net.pkhapps.idispatch.server.entity.repository;

import net.pkhapps.idispatch.server.entity.Assignment;
import net.pkhapps.idispatch.server.entity.DispatchNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository of {@link DispatchNotification}s.
 */
public interface DispatchNotificationRepository extends JpaRepository<DispatchNotification, Long> {

    List<DispatchNotification> findByAssignment(Assignment assignment);
}
