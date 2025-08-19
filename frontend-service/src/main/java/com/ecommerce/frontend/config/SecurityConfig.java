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

                        // Todos los endpoints de la aplicación requieren autenticación.
                        // Quitamos la regla que permitía públicamente /products y /products/{id}
                        // para forzar login antes de poder ver cualquier contenido.

                        // Edición de productos protegida
                        .requestMatchers(HttpMethod.GET, "/products/*/edit").authenticated()
                        .requestMatchers(HttpMethod.POST, "/products/*/edit").authenticated()

                        // Endpoint que usa el guard cliente para comprobar el estado de la sesión
                        // debe permanecer accesible para que el cliente reciba 401 cuando la
                        // sesión ha sido invalidada. Mantén esto si usas auth-guard.js.
                        .requestMatchers("/api/auth/check").permitAll()

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
