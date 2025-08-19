package com.ecommerce.frontend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtSessionFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(JwtSessionFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Object jwtObj = request.getSession().getAttribute("JWT");
        Object usernameObj = request.getSession().getAttribute("USERNAME");
        log.info("[JwtSessionFilter] JWT en sesión: {} | USERNAME en sesión: {}", jwtObj, usernameObj);
        if (jwtObj != null && usernameObj != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            String jwt = jwtObj.toString();
            String username = usernameObj.toString();
            log.info("[JwtSessionFilter] Estableciendo autenticación para usuario: {}", username);
            User principal = new User(username, "", Collections.emptyList());
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);
            log.debug("Autenticación establecida para usuario {} desde sesión JWT", username);
        } else {
            log.warn("[JwtSessionFilter] No se pudo establecer autenticación. JWT o USERNAME faltante, o ya autenticado.");
        }
        filterChain.doFilter(request, response);
    }
}
