package com.bankservice.bank_service.controller;

import com.bankservice.bank_service.domain.Merchant;
import com.bankservice.bank_service.repository.MerchantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/merchants")
public class MerchantController {

    @Autowired
    private MerchantRepository repo;

    @GetMapping("/{code}")
    public ResponseEntity<?> getByCode(@PathVariable("code") String code) {
        return repo.findByMerchantCode(code)
                .map(m -> ResponseEntity.ok(m))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Merchant m) {
        Merchant saved = repo.save(m);
        return ResponseEntity.status(201).body(saved);
    }
}
