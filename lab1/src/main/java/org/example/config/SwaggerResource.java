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
import java.nio.charset.StandardCharsets;
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
                .entity("Error redirecting to Swagger UI")
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
        return serveSwaggerIndex();
    }

    /**
     * Serves Swagger UI index.html
     */
    @GET
    @Path("/swagger-ui/index.html")
    @Produces(MediaType.TEXT_HTML)
    public Response swaggerUiIndex() {
        return serveSwaggerIndex();
    }

    /**
     * Serves WebJars static resources for Swagger UI
     */
    @GET
    @Path("/webjars/{path:.*}")
    public Response serveWebJars(@PathParam("path") String path) {
        try {
            // Construct the resource path for WebJars
            String resourcePath = "/META-INF/resources/webjars/" + path;
            
            InputStream resource = getClass().getResourceAsStream(resourcePath);
            if (resource == null) {
                log.warning("WebJar resource not found: " + resourcePath);
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            String contentType = getContentTypeFromPath(path);
            
            return Response.ok(resource, contentType)
                .header("Cache-Control", "public, max-age=31536000") // Cache for 1 year
                .build();
                
        } catch (Exception e) {
            log.severe("Error serving WebJar resource " + path + ": " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    private Response serveSwaggerIndex() {
        try {
            InputStream htmlStream = getClass().getResourceAsStream("/swagger-ui.html");
            if (htmlStream == null) {
                log.severe("swagger-ui.html template not found in resources");
                return Response.status(Response.Status.NOT_FOUND)
                    .entity("Swagger UI template not found")
                    .build();
            }

            String htmlContent = new String(htmlStream.readAllBytes(), StandardCharsets.UTF_8);
            
            // Replace placeholder with actual base URL
            String baseUrl = uriInfo.getBaseUri().toString();
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
            
            htmlContent = htmlContent.replace("{{API_BASE_URL}}", baseUrl);

            return Response.ok(htmlContent, MediaType.TEXT_HTML).build();
            
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