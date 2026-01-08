package org.example.config;

import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.util.logging.Logger;

import java.util.HashSet;
import java.util.Set;

@Path("/openapi.json")
public class OpenApiResource {

    private static final Logger log = Logger.getLogger(OpenApiResource.class.getName());

    @Context
    private Application application;

    @Context
    private UriInfo uriInfo;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOpenApi() {
        try {
            log.info("Generating OpenAPI specification");
            
            OpenAPI openAPI = createOpenApiConfig();
            
            SwaggerConfiguration config = new SwaggerConfiguration()
                .openAPI(openAPI)
                .prettyPrint(true)
                .resourcePackages(getResourcePackages())
                .scannerClass("io.swagger.v3.jaxrs2.integration.JaxrsAnnotationScanner");

            OpenAPI generatedOpenApi = new JaxrsOpenApiContextBuilder()
                .openApiConfiguration(config)
                .buildContext(true)
                .read();

            String openApiJson = io.swagger.v3.core.util.Json.pretty(generatedOpenApi);

            log.info("Successfully generated OpenAPI specification");
            return Response.ok(openApiJson)
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .header("Cache-Control", "public, max-age=300") // Cache for 5 minutes
                .build();
            
        } catch (Exception e) {
            log.severe("Failed to generate OpenAPI specification: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("{\"error\": \"Failed to generate OpenAPI specification\"}")
                .type(MediaType.APPLICATION_JSON)
                .build();
        }
    }

    private OpenAPI createOpenApiConfig() {
        String baseUrl = uriInfo.getBaseUri().toString();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        
        return new OpenAPI()
            .info(new Info()
                .title("Routes Management API")
                .version("1.0.0")
                .description("API для управления маршрутами, координатами и локациями. " +
                           "Позволяет создавать, редактировать, удалять и просматривать маршруты, " +
                           "а также выполнять специальные операции и импорт данных.")
                .contact(new Contact()
                    .name("API Support")
                    .email("support@example.com"))
                .license(new License()
                    .name("MIT License")
                    .url("https://opensource.org/licenses/MIT")))
            .addServersItem(new Server()
                .url(baseUrl)
                .description("Routes Management Server"));
    }

    private Set<String> getResourcePackages() {
        Set<String> packages = new HashSet<>();
        packages.add("org.example.domain.route.controller");
        packages.add("org.example.domain.import_history.controller");
        packages.add("org.example.domain.coordinates.controller");
        packages.add("org.example.domain.location.controller");
        return packages;
    }
}