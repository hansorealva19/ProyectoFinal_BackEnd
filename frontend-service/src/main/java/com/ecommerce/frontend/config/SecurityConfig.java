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
                                                                // Public: static assets, login and register. Everything else requires authentication.
                                                                        .requestMatchers("/favicon.ico", "/login", "/register", "/css/**", "/js/**", "/images/**").permitAll()
                                                                        .requestMatchers("/api/users/**").permitAll()
                                                                        .anyRequest().authenticated()
                                                        )
                                        // Use the custom login page at /login so unauthenticated requests are redirected there
                                        // but avoid Spring processing POST /login so our AuthController can handle it.
                                        .formLogin(form -> form.loginPage("/login").loginProcessingUrl("/perform_login").permitAll())
                                        .logout(logout -> logout
                                                .logoutUrl("/logout")
                                                .logoutSuccessUrl("/login?logout")
                                                .invalidateHttpSession(true)
                                                // delete the custom session cookie name we set in application.yml
                                                .deleteCookies("FRONTENDSESSIONID")
                                                .permitAll()
                                        )
                                        .addFilterBefore(new JwtSessionFilter(), org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);
                                return http.build();
    }
}
