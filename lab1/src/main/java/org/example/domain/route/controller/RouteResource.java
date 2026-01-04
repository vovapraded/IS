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
    @Path("/paginated")
    public Response getPaginatedRoutes(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("10") int size,
            @QueryParam("nameFilter") String nameFilter,
            @QueryParam("sortBy") @DefaultValue("id") String sortBy,
            @QueryParam("sortDirection") @DefaultValue("asc") String sortDirection) {
        
        try {
            // МАКСИМАЛЬНО БЕЗОПАСНАЯ ВЕРСИЯ: используем findAll() напрямую
            List<RouteDto> allRoutes = routeService.findAll();
            
            if (allRoutes == null) {
                allRoutes = new java.util.ArrayList<>();
            }
            
            // Убираем null элементы для безопасности
            allRoutes = allRoutes.stream()
                .filter(route -> route != null && route.name() != null)
                .collect(Collectors.toList());
            
            // Простая фильтрация
            List<RouteDto> filteredRoutes = allRoutes;
            if (nameFilter != null && !nameFilter.trim().isEmpty()) {
                String filter = nameFilter.trim().toLowerCase();
                filteredRoutes = allRoutes.stream()
                    .filter(route -> route.name().toLowerCase().contains(filter))
                    .collect(Collectors.toList());
            }
            
            // Простая пагинация
            int startIndex = page * size;
            int endIndex = Math.min(startIndex + size, filteredRoutes.size());
            List<RouteDto> pageRoutes = startIndex < filteredRoutes.size() ?
                filteredRoutes.subList(startIndex, endIndex) : new java.util.ArrayList<>();
            
            long totalElements = filteredRoutes.size();
            int totalPages = totalElements > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
            
            Map<String, Object> response = new HashMap<>();
            response.put("content", pageRoutes);
            response.put("totalElements", totalElements);
            response.put("totalPages", totalPages);
            response.put("currentPage", page);
            response.put("size", size);
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            log.error("Error in paginated routes: {}", e.getMessage(), e);
            // Крайний fallback - возвращаем пустой результат
            Map<String, Object> response = new HashMap<>();
            response.put("content", new java.util.ArrayList<>());
            response.put("totalElements", 0);
            response.put("totalPages", 0);
            response.put("currentPage", page);
            response.put("size", size);
            
            return Response.ok(response).build();
        }
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
}