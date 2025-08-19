package com.ecommerce.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
//@EnableEurekaClient // Enable Eureka Client for service discovery
public class ProductServiceApplication {

  public static void main(String[] args) {
    var context = SpringApplication.run(ProductServiceApplication.class, args);
    String port = context.getEnvironment().getProperty("server.port", "8080");
    System.out.println("Product Service is running... on http://localhost:" + port);
  }
}
