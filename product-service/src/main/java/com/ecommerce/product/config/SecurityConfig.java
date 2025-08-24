package com.ecommerce.product.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for product-service used to allow internal API calls
 * (order-service) to hit /api/** and actuator endpoints without being redirected
 * to a form-login page.
 *
 * Note: this is a pragmatic, low-risk change for a local/dev environment. For
 * production, secure inter-service calls with mTLS, API keys or an internal
 * gateway and avoid permitting anonymous access broadly.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
      .csrf(csrf -> csrf.disable())
      .authorizeHttpRequests(authorize -> authorize
        .requestMatchers("/api/**", "/actuator/**").permitAll()
        .anyRequest().authenticated()
      )
      // disable form login if present and keep basic auth available for diagnostics
      .formLogin(form -> form.disable())
      .httpBasic(Customizer.withDefaults());

    return http.build();
  }

}
