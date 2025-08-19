package com.bankservice.bank_service.repository;

import com.bankservice.bank_service.domain.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MerchantRepository extends JpaRepository<Merchant, Long> {
    Optional<Merchant> findByMerchantCode(String merchantCode);
}
