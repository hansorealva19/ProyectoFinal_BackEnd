// src/main/java/com/ecommerce/cart_service/CartServiceApplication.java
package com.ecommerce.cart_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class CartServiceApplication {

	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(CartServiceApplication.class, args);
		String port = context.getEnvironment().getProperty("server.port", "8080");
		System.out.println("Product Service is running... on http://localhost:" + port);
	}

}