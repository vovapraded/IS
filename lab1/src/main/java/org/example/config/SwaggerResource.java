package org.example.config;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.core.Context;

import java.io.InputStream;
import java.net.URI;
import java.util.logging.Logger;

@Path("/")
public class SwaggerResource {

    private static final Logger log = Logger.getLogger(SwaggerResource.class.getName());

    @Context
    private UriInfo uriInfo;

    /**
     * Redirects to Swagger UI
     */
    @GET
    @Path("/swagger")
    public Response swagger() {
        try {
            URI swaggerUiUri = uriInfo.getBaseUriBuilder()
                .path("swagger-ui")
                .build();
            
            return Response.temporaryRedirect(swaggerUiUri).build();
        } catch (Exception e) {
            log.severe("Error redirecting to Swagger UI: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("{\"error\": \"Error redirecting to Swagger UI\"}")
                .build();
        }
    }

    /**
     * Serves the main Swagger UI HTML page
     */
    @GET
    @Path("/swagger-ui")
    @Produces(MediaType.TEXT_HTML)
    public Response swaggerUi() {
        return createSwaggerUiResponse();
    }

    /**
     * Serves Swagger UI index.html
     */
    @GET
    @Path("/swagger-ui/index.html")
    @Produces(MediaType.TEXT_HTML)
    public Response swaggerUiIndex() {
        return createSwaggerUiResponse();
    }

    /**
     * Serves WebJars static resources for Swagger UI
     */
    @GET
    @Path("/webjars/{path:.*}")
    public Response serveWebJars(@PathParam("path") String path) {
        try {
            // Try direct WebJar path
            String resourcePath = "/META-INF/resources/webjars/" + path;
            
            InputStream resource = getClass().getResourceAsStream(resourcePath);
            if (resource == null) {
                log.warning("WebJar resource not found: " + resourcePath);
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            String contentType = getContentTypeFromPath(path);
            
            return Response.ok(resource, contentType)
                .header("Cache-Control", "public, max-age=31536000")
                .build();
                
        } catch (Exception e) {
            log.severe("Error serving WebJar resource " + path + ": " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    private Response createSwaggerUiResponse() {
        try {
            String baseUrl = uriInfo.getBaseUri().toString();
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
            
            // Create minimal HTML using WebJars resources
            String html = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Routes Management API - Swagger UI</title>
    <link rel="stylesheet" type="text/css" href="webjars/swagger-ui/5.10.3/swagger-ui.css" />
    <link rel="icon" type="image/png" href="webjars/swagger-ui/5.10.3/favicon-32x32.png" sizes="32x32" />
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
    <script src="webjars/swagger-ui/5.10.3/swagger-ui-bundle.js" charset="UTF-8"></script>
    <script src="webjars/swagger-ui/5.10.3/swagger-ui-standalone-preset.js" charset="UTF-8"></script>
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
                    document.getElementById('swagger-ui').innerHTML =
                        '<h2>Failed to load Swagger UI</h2>' +
                        '<p>Error: ' + JSON.stringify(err) + '</p>' +
                        '<p>Trying to load API spec from: <a href="%s/openapi.json">%s/openapi.json</a></p>';
                }
            });
            
            window.ui = ui;
        };
    </script>
</body>
</html>
            """.formatted(baseUrl, baseUrl, baseUrl, baseUrl);

            return Response.ok(html, MediaType.TEXT_HTML).build();
            
        } catch (Exception e) {
            log.severe("Error serving Swagger UI: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error loading Swagger UI")
                .build();
        }
    }

    private String getContentTypeFromPath(String path) {
        String lowerPath = path.toLowerCase();
        
        if (lowerPath.endsWith(".html")) {
            return MediaType.TEXT_HTML;
        } else if (lowerPath.endsWith(".css")) {
            return "text/css";
        } else if (lowerPath.endsWith(".js")) {
            return "application/javascript";
        } else if (lowerPath.endsWith(".json")) {
            return MediaType.APPLICATION_JSON;
        } else if (lowerPath.endsWith(".png")) {
            return "image/png";
        } else if (lowerPath.endsWith(".ico")) {
            return "image/x-icon";
        } else if (lowerPath.endsWith(".svg")) {
            return "image/svg+xml";
        } else if (lowerPath.endsWith(".woff") || lowerPath.endsWith(".woff2")) {
            return "font/woff";
        } else if (lowerPath.endsWith(".ttf")) {
            return "font/ttf";
        }
        
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}