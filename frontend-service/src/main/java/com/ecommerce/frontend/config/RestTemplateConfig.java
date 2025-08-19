package com.ecommerce.frontend.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate rt = new RestTemplate();
        // Interceptor para propagar JWT si existe en sesión
        rt.setInterceptors(List.of((request, body, execution) -> {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest httpReq = attrs.getRequest();
                Object jwt = httpReq.getSession().getAttribute("JWT");
                if (jwt != null) {
                    request.getHeaders().add("Authorization", "Bearer " + jwt.toString());
                }
            }
            return execution.execute(request, body);
        }));
        return rt;
    }
}
