package org.example.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;

@Provider
@PreMatching
@Slf4j
public class CorsFilter implements ContainerRequestFilter, ContainerResponseFilter {
    
    @Override
    public void filter(ContainerRequestContext requestContext) {
        log.info("CORS Filter: Processing request " + requestContext.getMethod() + " " +
                requestContext.getUriInfo().getPath());
        
        // Handle preflight OPTIONS request
        if ("OPTIONS".equals(requestContext.getMethod())) {
            log.info("CORS Filter: Handling OPTIONS preflight request");
            Response response = Response.ok()
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Headers", "origin, content-type, accept, authorization, x-requested-with")
                .header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD")
                .header("Access-Control-Max-Age", "3600")
                .build();
            requestContext.abortWith(response);
        }
    }
    
    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) {
        // Пропускаем OPTIONS запросы - они уже обработаны в request filter
        if ("OPTIONS".equals(request.getMethod())) {
            return;
        }
        
        log.info("CORS Filter: Adding CORS headers to response for " + request.getMethod() + " " +
                request.getUriInfo().getPath());
        
        // Проверяем, не установлены ли уже заголовки (избегаем дублирования)
        if (!response.getHeaders().containsKey("Access-Control-Allow-Origin")) {
            response.getHeaders().add("Access-Control-Allow-Origin", "*");
        }
        if (!response.getHeaders().containsKey("Access-Control-Allow-Headers")) {
            response.getHeaders().add("Access-Control-Allow-Headers", "origin, content-type, accept, authorization, x-requested-with");
        }
        if (!response.getHeaders().containsKey("Access-Control-Allow-Methods")) {
            response.getHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
        }
        if (!response.getHeaders().containsKey("Access-Control-Max-Age")) {
            response.getHeaders().add("Access-Control-Max-Age", "3600");
        }
    }
}