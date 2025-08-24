package order.repository;

import order.domain.StockUpdateFailure;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockUpdateFailureRepository extends JpaRepository<StockUpdateFailure, Long> {

}
