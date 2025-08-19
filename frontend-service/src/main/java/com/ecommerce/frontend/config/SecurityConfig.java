package com.ecommerce.frontend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                                http
                                        .csrf(csrf -> csrf.disable())
                                                        .authorizeHttpRequests(auth -> auth
                                                                .requestMatchers("/", "/home", "/favicon.ico", "/login", "/register", "/css/**", "/js/**", "/images/**").permitAll()
                                                                                                .requestMatchers("/products", "/products/**", "/cart", "/cart/**", "/orders", "/orders/**").permitAll()
                                                                .requestMatchers("/api/products/**", "/api/users/**").permitAll()
                                                                .anyRequest().authenticated()
                                                        )
                                        .logout(logout -> logout
                                                .logoutUrl("/logout")
                                                .logoutSuccessUrl("/login?logout")
                                                .invalidateHttpSession(true)
                                                .deleteCookies("JSESSIONID")
                                                .permitAll()
                                        )
                                        .addFilterBefore(new JwtSessionFilter(), org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);
                                return http.build();
    }
}
