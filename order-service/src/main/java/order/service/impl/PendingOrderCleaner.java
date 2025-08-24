package order.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import order.repository.OrderRepository;
import order.service.AuditService;
import order.domain.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class PendingOrderCleaner {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private AuditService auditService;

    // Runs every 30 seconds and cancels orders pending for more than 1 minute
    @Scheduled(fixedDelayString = "${orders.cleanup.ms:30000}")
    public void cleanup() {
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusMinutes(1);
            List<order.domain.Order> stale = orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.PENDING, cutoff);
            for (order.domain.Order o : stale) {
                    try {
                    o.cancel("Expired after 1 minute");
                    orderRepository.save(o);
                    try { auditService.record(o.getUserName(), "ORDER_CANCELLED", "Expired order id:"+o.getId()); } catch (Exception e) { org.slf4j.LoggerFactory.getLogger(PendingOrderCleaner.class).warn("Failed to record audit for expired order: {}", e.getMessage()); }
                } catch (Exception ex) {
                    org.slf4j.LoggerFactory.getLogger(PendingOrderCleaner.class).warn("Failed to cancel expired order {}: {}", o.getId(), ex.getMessage());
                }
            }
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(PendingOrderCleaner.class).error("PendingOrderCleaner failed: {}", e.getMessage(), e);
        }
    }
}
