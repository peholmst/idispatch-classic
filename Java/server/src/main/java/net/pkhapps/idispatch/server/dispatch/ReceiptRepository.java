package net.pkhapps.idispatch.server.dispatch;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository of {@link Receipt}s.
 */
public interface ReceiptRepository extends JpaRepository<Receipt, Long> {

    List<Receipt> findByNotification(DispatchNotification notification);

    List<Receipt> findByDestination(Destination destination);

}
