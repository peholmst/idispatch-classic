package net.pkhapps.idispatch.server.dispatch;

import net.pkhapps.idispatch.server.assignment.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository of {@link DispatchNotification}s.
 */
public interface DispatchNotificationRepository extends JpaRepository<DispatchNotification, Long> {

    List<DispatchNotification> findByAssignment(Assignment assignment);
}
