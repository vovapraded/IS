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
import org.example.exception.ValidationException;
import org.example.exception.RouteNameAlreadyExistsException;
import org.example.exception.RouteCoordinatesAlreadyExistException;
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
            List<RouteDto> routes = routeService.findPaginated(page, size, nameFilter, sortBy, sortDirection);
            long totalElements = routeService.countWithFilter(nameFilter);
            int totalPages = (int) Math.ceil((double) totalElements / size);
            
            Map<String, Object> response = new HashMap<>();
            response.put("content", routes);
            response.put("page", page);
            response.put("size", size);
            response.put("totalElements", totalElements);
            response.put("totalPages", totalPages);
            response.put("first", page == 0);
            response.put("last", page >= totalPages - 1);
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            log.error("Pagination error: {}", e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Pagination system error: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/{id}")
    public RouteDto getById(@PathParam("id") Integer id) {
        return routeService.findById(id);
    }

    @POST
    public Response create(RouteCreateDto dto) {
        log.info("🚀 CONTROLLER: Received request to create route: {}", dto);
        
        // ПРОВЕРКА ПРЯМО В КОНТРОЛЛЕРЕ - до EJB
        if (dto.coordinates() != null) {
            log.info("🔍 CONTROLLER: Pre-checking coordinates uniqueness: ({}, {})",
                    dto.coordinates().x(), dto.coordinates().y());
            try {
                // Прямая проверка через сервис без создания маршрута
                List<RouteDto> allRoutes = routeService.findAll();
                for (RouteDto existingRoute : allRoutes) {
                    if (existingRoute.coordinates() != null &&
                        Float.compare(existingRoute.coordinates().x(), dto.coordinates().x()) == 0 &&
                        Double.compare(existingRoute.coordinates().y(), dto.coordinates().y()) == 0) {
                        
                        log.error("❌ CONTROLLER: Found duplicate coordinates in route ID: {}", existingRoute.id());
                        return Response.status(Response.Status.CONFLICT)
                                .entity(Map.of(
                                    "error", "Маршрут с координатами (" + dto.coordinates().x() + ", " + dto.coordinates().y() + ") уже существует",
                                    "type", "DUPLICATE_COORDINATES",
                                    "existingRouteId", existingRoute.id()
                                ))
                                .build();
                    }
                }
                log.info("✅ CONTROLLER: Pre-check passed - coordinates are unique");
            } catch (Exception e) {
                log.warn("⚠️ CONTROLLER: Error during pre-check, continuing with service call: {}", e.getMessage());
            }
        }
        
        try {
            RouteDto created = routeService.createRoute(dto);
            log.info("✅ CONTROLLER: Route created successfully: {}", created.id());
            return Response.status(Response.Status.CREATED).entity(created).build();
        } catch (RouteNameAlreadyExistsException e) {
            log.warn("❌ CONTROLLER: Route name already exists: {}", e.getMessage());
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("error", e.getMessage(), "type", "DUPLICATE_NAME"))
                    .build();
        } catch (RouteCoordinatesAlreadyExistException e) {
            log.warn("📍 CONTROLLER: Route coordinates already exist: {}", e.getMessage());
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("error", e.getMessage(), "type", "DUPLICATE_COORDINATES"))
                    .build();
        } catch (ValidationException e) {
            log.warn("⚠️ CONTROLLER: Validation error during route creation: {}", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage(), "type", "VALIDATION_ERROR"))
                    .build();
        } catch (IllegalArgumentException e) {
            log.warn("🔧 CONTROLLER: Invalid argument during route creation: {}", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage(), "type", "INVALID_ARGUMENT"))
                    .build();
        } catch (Exception e) {
            log.error("💥 CONTROLLER: Unexpected error - Type: {}, Message: {}, Cause: {}",
                    e.getClass().getName(), e.getMessage(), e.getCause() != null ? e.getCause().getClass().getName() : "null", e);
            
            // Проверяем, является ли причина исключения нашим кастомным исключением
            Throwable rootCause = e;
            while (rootCause.getCause() != null) {
                rootCause = rootCause.getCause();
                log.error("💥 CONTROLLER: Root cause - Type: {}, Message: {}", rootCause.getClass().getName(), rootCause.getMessage());
                
                // Проверяем конкретные типы исключений в цепочке причин
                if (rootCause instanceof RouteNameAlreadyExistsException) {
                    log.warn("🎯 CONTROLLER: Found RouteNameAlreadyExistsException in cause chain: {}", rootCause.getMessage());
                    return Response.status(Response.Status.CONFLICT)
                            .entity(Map.of("error", rootCause.getMessage(), "type", "DUPLICATE_NAME"))
                            .build();
                } else if (rootCause instanceof RouteCoordinatesAlreadyExistException) {
                    log.warn("🎯 CONTROLLER: Found RouteCoordinatesAlreadyExistException in cause chain: {}", rootCause.getMessage());
                    return Response.status(Response.Status.CONFLICT)
                            .entity(Map.of("error", rootCause.getMessage(), "type", "DUPLICATE_COORDINATES"))
                            .build();
                }
            }
            
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Не удалось сохранить маршрут. Попробуйте еще раз.", "type", "INTERNAL_ERROR"))
                    .build();
        }
    }

    @GET
    @Path("/test-validation")
    public Response testValidation() {
        log.info("🧪 TEST ENDPOINT CALLED");
        return Response.ok(Map.of(
            "message", "НОВЫЙ КОД РАБОТАЕТ! Валидация должна функционировать.",
            "timestamp", System.currentTimeMillis(),
            "version", "v2-with-validation"
        )).build();
    }
    
    @POST
    @Path("/test-coordinates-validation")
    public Response testCoordinatesValidation(@QueryParam("x") Float x, @QueryParam("y") Double y) {
        log.info("🧪🔍 TEST COORDINATES VALIDATION: Testing coordinates ({}, {})", x, y);
        
        try {
            log.info("🧪 Creating test route with coordinates ({}, {})", x, y);
            
            RouteCreateDto testDto = new RouteCreateDto(
                "TEST_COORDINATES_" + System.currentTimeMillis(), // уникальное имя
                new org.example.domain.coordinates.dto.CoordinatesDto(null, x, y, null, null),
                new org.example.domain.location.dto.LocationDto(null, 1.0, 1.0, "Test From", null, null),
                new org.example.domain.location.dto.LocationDto(null, 2.0, 2.0, "Test To", null, null),
                100L,
                5L
            );
            
            RouteDto created = routeService.createRoute(testDto);
            log.info("✅🧪 TEST: Route created successfully with coordinates ({}, {}) - Route ID: {}", x, y, created.id());
            
            return Response.ok(Map.of(
                "result", "SUCCESS",
                "message", "Координаты (" + x + ", " + y + ") уникальны - маршрут создан",
                "routeId", created.id(),
                "coordinates", "(" + x + ", " + y + ")"
            )).build();
            
        } catch (RouteCoordinatesAlreadyExistException e) {
            log.warn("❌🧪 TEST: Coordinates validation failed: {}", e.getMessage());
            return Response.status(Response.Status.CONFLICT).entity(Map.of(
                "result", "DUPLICATE_COORDINATES",
                "message", e.getMessage(),
                "coordinates", "(" + x + ", " + y + ")"
            )).build();
            
        } catch (Exception e) {
            log.error("💥🧪 TEST: Unexpected error during coordinates test: {}", e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Map.of(
                "result", "ERROR",
                "message", "Ошибка при тестировании координат: " + e.getMessage(),
                "error_type", e.getClass().getSimpleName(),
                "coordinates", "(" + x + ", " + y + ")"
            )).build();
        }
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") Integer id, RouteUpdateDto dto) {
        log.info("Received request to update route {}: {}", id, dto);
        if (!dto.id().equals(id)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "ID в URL и в теле запроса не совпадают"))
                    .build();
        }
        
        try {
            RouteDto updated = routeService.updateRoute(dto);
            log.info("Route updated successfully: {}", updated.id());
            return Response.ok(updated).build();
        } catch (RouteNameAlreadyExistsException e) {
            log.warn("❌ Route name already exists on update: {}", e.getMessage());
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("error", e.getMessage(), "type", "DUPLICATE_NAME"))
                    .build();
        } catch (RouteCoordinatesAlreadyExistException e) {
            log.warn("📍 Route coordinates already exist on update: {}", e.getMessage());
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("error", e.getMessage(), "type", "DUPLICATE_COORDINATES"))
                    .build();
        } catch (ValidationException e) {
            log.warn("⚠️ Validation error during route update: {}", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage(), "type", "VALIDATION_ERROR"))
                    .build();
        } catch (IllegalArgumentException e) {
            log.warn("🔧 Invalid argument during route update: {}", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage(), "type", "INVALID_ARGUMENT"))
                    .build();
        } catch (Exception e) {
            log.error("💥 Unexpected error during route update: {}", e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Не удалось обновить маршрут. Попробуйте еще раз.", "type", "INTERNAL_ERROR"))
                    .build();
        }
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