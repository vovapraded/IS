package org.example.domain.route.controller;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.example.domain.route.dto.*;
import org.example.domain.route.service.RouteServiceMB;
import org.example.domain.coordinates.dto.CoordinatesDto;
import org.example.domain.location.dto.LocationDto;
import org.example.domain.import_history.service.RouteImportServiceMB;
import org.example.domain.import_history.dto.ImportRequestDto;
import org.example.domain.import_history.dto.ImportResultDto;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/routes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Slf4j
public class RouteResource {

    @Inject
    private RouteServiceMB routeService;
    
    @Inject
    private RouteImportServiceMB routeImportService;

    @GET
    public List<RouteDto> getAllRoutes() {
        return routeService.findAll();
    }

    @GET
    @Path("/cursor")
    public Response getCursorPaginatedRoutes(
            @QueryParam("size") @DefaultValue("10") int size,
            @QueryParam("nameFilter") String nameFilter,
            @QueryParam("sortBy") @DefaultValue("id") String sortBy,
            @QueryParam("sortDirection") @DefaultValue("asc") String sortDirection,
            @QueryParam("cursor") String cursor,
            @QueryParam("direction") @DefaultValue("next") String direction) {
        
        try {
            RouteCursorPageDto result;
            
            if (cursor == null || cursor.trim().isEmpty()) {
                // Первая страница
                log.info("🚀 First page: size={}, filter='{}', sortBy={}, direction={}",
                        size, nameFilter, sortBy, sortDirection);
                result = routeService.findFirstPage(size, nameFilter, sortBy, sortDirection);
            } else {
                // Навигация по композитному cursor
                log.info("🔄 {} page: cursor='{}...', size={}, filter='{}'",
                        direction, cursor.substring(0, Math.min(cursor.length(), 20)), size, nameFilter);
                
                if ("prev".equals(direction)) {
                    result = routeService.findPrevPage(cursor, size, nameFilter);
                } else {
                    result = routeService.findNextPage(cursor, size, nameFilter);
                }
            }
            
            // Чистый cursor-based API response
            Map<String, Object> response = new HashMap<>();
            response.put("content", result.routes());
            response.put("size", result.size());
            response.put("totalCount", result.totalCount());
            response.put("hasNext", result.hasNext());
            response.put("hasPrev", result.hasPrev());
            response.put("nextCursor", result.nextCursor());
            response.put("prevCursor", result.prevCursor());
            
            log.info("✅ Returned {} routes (hasNext={}, hasPrev={})",
                    result.routes().size(), result.hasNext(), result.hasPrev());
            
            return Response.ok(response).build();
            
        } catch (IllegalArgumentException e) {
            log.warn("❌ Invalid cursor, returning first page: {}", e.getMessage());
            
            // При невалидном cursor возвращаем первую страницу
            RouteCursorPageDto fallbackResult = routeService.findFirstPage(size, nameFilter, sortBy, sortDirection);
            
            Map<String, Object> response = new HashMap<>();
            response.put("content", fallbackResult.routes());
            response.put("size", fallbackResult.size());
            response.put("totalCount", fallbackResult.totalCount());
            response.put("hasNext", fallbackResult.hasNext());
            response.put("hasPrev", false);
            response.put("nextCursor", fallbackResult.nextCursor());
            response.put("prevCursor", null);
            response.put("warning", "Invalid cursor, showing first page");
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            log.error("💥 Cursor pagination error: {}", e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Pagination system error: " + e.getMessage()))
                    .build();
        }
    }
    
    @GET
    @Path("/paginated")
    public Response getLegacyPaginatedRoutes(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("10") int size,
            @QueryParam("nameFilter") String nameFilter,
            @QueryParam("sortBy") @DefaultValue("id") String sortBy,
            @QueryParam("sortDirection") @DefaultValue("asc") String sortDirection) {
        
        // Перенаправляем на cursor API
        log.info("🔄 Legacy pagination request redirected to cursor API");
        return getCursorPaginatedRoutes(size, nameFilter, sortBy, sortDirection, null, "next");
    }

    // Остальные методы остаются без изменений
    @GET
    @Path("/{id}")
    public RouteDto getById(@PathParam("id") Integer id) {
        return routeService.findById(id);
    }

    @POST
    public Response create(RouteCreateDto dto) {
        RouteDto created = routeService.createRoute(dto);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{id}")
    public RouteDto update(@PathParam("id") Integer id, RouteUpdateDto dto) {
        if (!dto.id().equals(id)) {
            throw new BadRequestException("Path ID and body ID must match");
        }
        return routeService.updateRoute(dto);
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Integer id,
                          @QueryParam("coordinatesTargetRouteId") Integer coordinatesTargetRouteId,
                          @QueryParam("fromLocationTargetRouteId") Integer fromLocationTargetRouteId,
                          @QueryParam("toLocationTargetRouteId") Integer toLocationTargetRouteId,
                          @QueryParam("targetRouteId") Integer targetRouteId) {
        if (targetRouteId != null) {
            routeService.deleteWithRebinding(id, targetRouteId, targetRouteId, targetRouteId);
        } else if (coordinatesTargetRouteId != null || fromLocationTargetRouteId != null || toLocationTargetRouteId != null) {
            routeService.deleteWithRebinding(id, coordinatesTargetRouteId, fromLocationTargetRouteId, toLocationTargetRouteId);
        } else {
            routeService.delete(id);
        }
        return Response.noContent().build();
    }
    // Эндпоинты для загрузки связанных данных для форм
    @GET
    @Path("/related/coordinates")
    public Response getAvailableCoordinates() {
        try {
            List<CoordinatesDto> coordinates = routeService.getAvailableCoordinates();
            return Response.ok(coordinates).build();
        } catch (Exception e) {
            log.error("Error loading coordinates: {}", e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to load coordinates"))
                    .build();
        }
    }
    
    @GET
    @Path("/related/locations")
    public Response getAvailableLocations() {
        try {
            List<LocationDto> locations = routeService.getAvailableLocations();
            return Response.ok(locations).build();
        } catch (Exception e) {
            log.error("Error loading locations: {}", e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to load locations"))
                    .build();
        }
    }
}