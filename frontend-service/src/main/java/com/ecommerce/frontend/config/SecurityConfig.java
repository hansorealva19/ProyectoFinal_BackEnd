package com.ecommerce.frontend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpMethod;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtSessionFilter jwtSessionFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Público: estáticos, login, register
                        .requestMatchers("/favicon.ico", "/login", "/register", "/css/**", "/js/**", "/images/**").permitAll()

                        // Listado y detalle de productos públicos
                        // NOTA: "/products/*" cubre "/products/{id}" pero NO cubre "/products/{id}/edit"
                        .requestMatchers(HttpMethod.GET, "/products", "/products/*").permitAll()

                        // Edición de productos protegida
                        .requestMatchers(HttpMethod.GET, "/products/*/edit").authenticated()
                        .requestMatchers(HttpMethod.POST, "/products/*/edit").authenticated()

                        // Usuarios públicos (ajusta según tu caso real)
                        .requestMatchers("/api/users/**").permitAll()

                        // Todo lo demás autenticado
                        .anyRequest().authenticated()
                )
                // Login y Logout
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/perform_login")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("FRONTENDSESSIONID")
                        .permitAll()
                )
                // Coloca el filtro que carga la autenticación desde la sesión (JWT guardado)
                .addFilterBefore(jwtSessionFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
