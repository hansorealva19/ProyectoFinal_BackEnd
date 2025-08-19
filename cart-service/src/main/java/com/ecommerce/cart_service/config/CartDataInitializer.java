package com.ecommerce.cart_service.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@Component
public class CartDataInitializer {
    private static final Logger log = LoggerFactory.getLogger(CartDataInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public CartDataInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void populateProductCategory() {
        try {
            log.info("Populating cart_item.product_category from products table if missing...");
            String sql = "UPDATE cart_item c JOIN ecommerce_products.products p ON c.product_id = p.id SET c.product_category = p.category WHERE c.product_category IS NULL";
            int updated = jdbcTemplate.update(sql);
            log.info("cart_item.product_category updated rows: {}", updated);
        } catch (Exception ex) {
            log.warn("Could not populate cart_item.product_category automatically: {}", ex.getMessage());
        }
    }
}
