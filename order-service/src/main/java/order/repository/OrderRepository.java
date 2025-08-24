package order.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import order.domain.Order;

/** Spring Data JPA repository for Order entity. */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
	Page<Order> findByUserName(String userName, Pageable pageable);
	java.util.List<order.domain.Order> findByStatusAndCreatedAtBefore(order.domain.OrderStatus status, java.time.LocalDateTime before);
	java.util.List<order.domain.Order> findByUserIdAndStatus(Long userId, order.domain.OrderStatus status);
}
