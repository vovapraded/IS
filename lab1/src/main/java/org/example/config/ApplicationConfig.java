package org.example.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import org.example.filter.CorsFilter;

import java.util.HashSet;
import java.util.Set;

@ApplicationPath("/api")
@OpenAPIDefinition(
    info = @Info(
        title = "Routes Management API",
        version = "1.0.0",
        description = "API для управления маршрутами, координатами и локациями",
        contact = @Contact(
            name = "API Support",
            email = "support@example.com"
        ),
        license = @License(
            name = "MIT License",
            url = "https://opensource.org/licenses/MIT"
        )
    ),
    servers = {
        @Server(url = "/lab1/api", description = "Routes Management Server"),
        @Server(url = "/api", description = "Fallback Server")
    }
)
public class ApplicationConfig extends Application {
    
    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();
        // Добавляем фильтр CORS
        classes.add(CorsFilter.class);
        
        // Явно добавляем контроллеры
        classes.add(org.example.domain.route.controller.RouteResource.class);
        classes.add(org.example.domain.route.controller.SpecialOperationsResource.class);
        classes.add(org.example.domain.coordinates.controller.CoordinatesResource.class);
        classes.add(org.example.domain.location.controller.LocationResource.class);
        classes.add(org.example.domain.import_history.controller.ImportResource.class);
        
        // Добавляем Swagger ресурсы
        classes.add(org.example.config.OpenApiResource.class);
        classes.add(org.example.config.SwaggerResource.class);
        
        return classes;
    }
}