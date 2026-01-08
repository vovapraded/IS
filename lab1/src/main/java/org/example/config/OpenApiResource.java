package org.example.config;

import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.oas.integration.OpenApiConfigurationException;
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
import java.util.stream.Collectors;

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
            
            // Configure Swagger
            SwaggerConfiguration config = new SwaggerConfiguration()
                .openAPI(openAPI)
                .prettyPrint(true)
                .resourcePackages(getResourcePackages())
                .scannerClass("io.swagger.v3.jaxrs2.integration.JaxrsAnnotationScanner");

            try {
                OpenAPI generatedOpenApi = new JaxrsOpenApiContextBuilder()
                    .openApiConfiguration(config)
                    .buildContext(true)
                    .read();

                // Преобразуем в JSON строку с корректным форматированием
                String openApiJson = io.swagger.v3.core.util.Json.pretty(generatedOpenApi);

                log.info("Successfully generated OpenAPI specification");
                return Response.ok(openApiJson)
                    .header("Content-Type", MediaType.APPLICATION_JSON)
                    .build();
            } catch (Exception swaggerException) {
                log.warning("Swagger generation failed, falling back to manual OpenAPI creation: " + swaggerException.getMessage());
                
                // Fallback: Создаем простую OpenAPI спецификацию вручную
                String fallbackJson = createFallbackOpenApiJson();
                return Response.ok(fallbackJson)
                    .header("Content-Type", MediaType.APPLICATION_JSON)
                    .build();
            }
            
        } catch (Exception e) {
            log.severe("Unexpected error generating OpenAPI specification: " + e.getMessage());
            
            // Последний fallback - создаем минимальную рабочую спецификацию
            String minimalJson = createMinimalOpenApiJson();
            return Response.ok(minimalJson).build();
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

    private String createFallbackOpenApiJson() {
        String baseUrl = uriInfo.getBaseUri().toString();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        
        return """
        {
          "openapi": "3.0.1",
          "info": {
            "title": "Routes Management API",
            "description": "API для управления маршрутами, координатами и локациями",
            "version": "1.0.0",
            "contact": {
              "name": "API Support",
              "email": "support@example.com"
            }
          },
          "servers": [
            {
              "url": "%s",
              "description": "Routes Management Server"
            }
          ],
          "paths": {
            "/routes": {
              "get": {
                "tags": ["Routes"],
                "summary": "Получить все маршруты",
                "responses": {
                  "200": {
                    "description": "Успешный ответ"
                  }
                }
              },
              "post": {
                "tags": ["Routes"],
                "summary": "Создать новый маршрут",
                "responses": {
                  "201": {
                    "description": "Маршрут создан"
                  }
                }
              }
            },
            "/routes/{id}": {
              "get": {
                "tags": ["Routes"],
                "summary": "Получить маршрут по ID",
                "parameters": [
                  {
                    "name": "id",
                    "in": "path",
                    "required": true,
                    "schema": {
                      "type": "integer"
                    }
                  }
                ],
                "responses": {
                  "200": {
                    "description": "Маршрут найден"
                  },
                  "404": {
                    "description": "Маршрут не найден"
                  }
                }
              },
              "put": {
                "tags": ["Routes"],
                "summary": "Обновить маршрут",
                "parameters": [
                  {
                    "name": "id",
                    "in": "path",
                    "required": true,
                    "schema": {
                      "type": "integer"
                    }
                  }
                ],
                "responses": {
                  "200": {
                    "description": "Маршрут обновлен"
                  }
                }
              },
              "delete": {
                "tags": ["Routes"],
                "summary": "Удалить маршрут",
                "parameters": [
                  {
                    "name": "id",
                    "in": "path",
                    "required": true,
                    "schema": {
                      "type": "integer"
                    }
                  }
                ],
                "responses": {
                  "204": {
                    "description": "Маршрут удален"
                  }
                }
              }
            },
            "/routes/special/max-name": {
              "get": {
                "tags": ["Special Operations"],
                "summary": "Найти маршрут с максимальным именем",
                "responses": {
                  "200": {
                    "description": "Успешный ответ"
                  }
                }
              }
            },
            "/import/routes": {
              "post": {
                "tags": ["Import"],
                "summary": "Импортировать маршруты",
                "responses": {
                  "200": {
                    "description": "Импорт выполнен"
                  }
                }
              }
            }
          }
        }
        """.formatted(baseUrl);
    }

    private String createMinimalOpenApiJson() {
        return """
        {
          "openapi": "3.0.1",
          "info": {
            "title": "Routes Management API",
            "version": "1.0.0"
          },
          "paths": {}
        }
        """;
    }
}