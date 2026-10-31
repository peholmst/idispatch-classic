package net.pkhapps.idispatch.server.dispatch;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository of {@link DispatchNotification}s.
 */
public interface DispatchNotificationRepository extends JpaRepository<DispatchNotification, Long> {

    List<DispatchNotification> findByAssignmentId(Long assignmentId);
}
