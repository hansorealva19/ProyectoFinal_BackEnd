package com.paymentservice.payment_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI paymentOpenAPI() {
        return new OpenAPI().info(new Info().title("Payment Service API").version("1.0.0"));
    }

    @Bean
    public GroupedOpenApi paymentApi() {
        return GroupedOpenApi.builder()
                .group("payments")
                .pathsToMatch("/api/payments/**")
                .build();
    }
}
