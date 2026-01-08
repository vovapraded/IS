package org.example.config;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.core.Context;

import java.io.InputStream;
import java.net.URI;

@Path("/")
public class SwaggerResource {

    @Context
    private UriInfo uriInfo;

    /**
     * Redirects to Swagger UI
     */
    @GET
    @Path("/swagger")
    public Response swagger() {
        try {
            // Перенаправляем на Swagger UI с правильным URL нашей OpenAPI спецификации
            URI swaggerUiUri = uriInfo.getBaseUriBuilder()
                .path("swagger-ui")
                .path("index.html")
                .queryParam("url", uriInfo.getBaseUri() + "api/openapi.json")
                .build();
            
            return Response.temporaryRedirect(swaggerUiUri).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error redirecting to Swagger UI: " + e.getMessage())
                .build();
        }
    }

    /**
     * Serves Swagger UI static files
     */
    @GET
    @Path("/swagger-ui")
    public Response swaggerUi() {
        String baseUri = uriInfo.getBaseUri().toString();
        if (baseUri.endsWith("/")) {
            baseUri = baseUri.substring(0, baseUri.length() - 1);
        }
        
        String customHtml = createCustomSwaggerHtml(baseUri);
        return Response.ok(customHtml, MediaType.TEXT_HTML).build();
    }

    /**
     * Serves Swagger UI static files with path - всё через CDN, кроме index.html
     */
    @GET
    @Path("/swagger-ui/{path:.*}")
    public Response swaggerUiResource(@jakarta.ws.rs.PathParam("path") String path) {
        try {
            // Для index.html или пустого пути возвращаем наш HTML
            if (path == null || path.isEmpty() || path.equals("/") || path.equals("index.html")) {
                String baseUri = uriInfo.getBaseUri().toString();
                if (baseUri.endsWith("/")) {
                    baseUri = baseUri.substring(0, baseUri.length() - 1);
                }
                
                String customHtml = createCustomSwaggerHtml(baseUri);
                return Response.ok(customHtml, MediaType.TEXT_HTML).build();
            }

            // Для всех остальных файлов - редирект на CDN
            return Response.temporaryRedirect(
                java.net.URI.create("https://unpkg.com/swagger-ui-dist@5.10.3/" + path)
            ).build();
            
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error serving Swagger UI: " + e.getMessage())
                .build();
        }
    }

    private String createCustomSwaggerHtml(String baseUri) {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Routes Management API - Swagger UI</title>
    <link rel="stylesheet" type="text/css" href="https://unpkg.com/swagger-ui-dist@5.10.3/swagger-ui.css" />
    <link rel="icon" type="image/png" href="https://unpkg.com/swagger-ui-dist@5.10.3/favicon-32x32.png" sizes="32x32" />
    <style>
        html {
            box-sizing: border-box;
            overflow: -moz-scrollbars-vertical;
            overflow-y: scroll;
        }
        *, *:before, *:after {
            box-sizing: inherit;
        }
        body {
            margin:0;
            background: #fafafa;
        }
    </style>
</head>
<body>
    <div id="swagger-ui"></div>
    <script src="https://unpkg.com/swagger-ui-dist@5.10.3/swagger-ui-bundle.js" charset="UTF-8"></script>
    <script src="https://unpkg.com/swagger-ui-dist@5.10.3/swagger-ui-standalone-preset.js" charset="UTF-8"></script>
    <script>
        window.onload = function() {
            console.log('Loading Swagger UI...');
            console.log('API URL: %s/openapi.json');
            
            const ui = SwaggerUIBundle({
                url: '%s/openapi.json',
                dom_id: '#swagger-ui',
                deepLinking: true,
                presets: [
                    SwaggerUIBundle.presets.apis,
                    SwaggerUIStandalonePreset
                ],
                plugins: [
                    SwaggerUIBundle.plugins.DownloadUrl
                ],
                layout: "StandaloneLayout",
                validatorUrl: null,
                onComplete: function() {
                    console.log('Swagger UI loaded successfully');
                },
                onFailure: function(err) {
                    console.error('Failed to load Swagger UI:', err);
                    // Показываем ошибку на странице
                    document.getElementById('swagger-ui').innerHTML =
                        '<h2>Failed to load Swagger UI</h2><p>Error: ' + JSON.stringify(err) + '</p>' +
                        '<p>Trying to load API spec from: <a href="%s/openapi.json">%s/openapi.json</a></p>';
                }
            });
            
            window.ui = ui;
        };
    </script>
</body>
</html>
        """.formatted(baseUri, baseUri, baseUri, baseUri);
    }

    private String getContentType(String path) {
        if (path.endsWith(".html")) {
            return MediaType.TEXT_HTML;
        } else if (path.endsWith(".css")) {
            return "text/css";
        } else if (path.endsWith(".js")) {
            return "application/javascript";
        } else if (path.endsWith(".json")) {
            return MediaType.APPLICATION_JSON;
        } else if (path.endsWith(".png")) {
            return "image/png";
        } else if (path.endsWith(".ico")) {
            return "image/x-icon";
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}