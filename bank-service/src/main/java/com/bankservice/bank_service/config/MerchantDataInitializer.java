package com.bankservice.bank_service.config;

import com.bankservice.bank_service.domain.Merchant;
import com.bankservice.bank_service.repository.MerchantRepository;
import com.bankservice.bank_service.entity.Account;
import com.bankservice.bank_service.repository.AccountRepository;
import java.math.BigDecimal;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MerchantDataInitializer {

    @Bean
    public CommandLineRunner initMerchants(MerchantRepository repo, AccountRepository accountRepo) {
        return args -> {
            String acctNum = "5810588803";
            // ensure account exists
            Account acc = accountRepo.findByAccountNumber(acctNum).orElseGet(() -> {
                Account a = Account.builder()
                        .accountNumber(acctNum)
                        .balance(BigDecimal.ZERO)
                        .bankCode("MIBANK")
                        .bankName("Mi Banco")
                        .build();
                return accountRepo.save(a);
            });

            var existing = repo.findByMerchantCode("ecommerce");
            if (existing.isEmpty()) {
                Merchant m = new Merchant("ecommerce", acc.getId(), "Default Ecommerce Merchant");
                repo.save(m);
                System.out.println("[MerchantDataInitializer] Created default merchant 'ecommerce' -> accountId=" + acc.getId());
            } else {
                Merchant m = existing.get();
                try {
                    // verify referenced account exists
                    accountRepo.findById(m.getAccountId()).orElseThrow(() -> new RuntimeException("Missing"));
                } catch (Exception e) {
                    // referenced account missing, update merchant to point to the created account
                    m.setAccountId(acc.getId());
                    repo.save(m);
                    System.out.println("[MerchantDataInitializer] Fixed merchant 'ecommerce' to point to accountId=" + acc.getId());
                }
            }
        };
    }
}
