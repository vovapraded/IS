package org.example.domain.route.controller;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.route.dto.RouteDto;
import org.example.domain.route.dto.RouteErrorType;
import org.example.domain.route.service.RouteServiceMB;
import org.example.exception.RouteNameAlreadyExistsException;
import org.example.exception.RouteZeroDistanceException;
import org.example.exception.ValidationException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/routes/special")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Slf4j
public class SpecialOperationsResource {

    @Inject
    private RouteServiceMB routeService;

    /**
     * Найти маршрут с максимальным именем (лексикографически)
     */
    @GET
    @Path("/max-name")
    public Response findRouteWithMaxName() {
        try {
            RouteDto route = routeService.findRouteWithMaxName();
            Map<String, Object> response = new HashMap<>();
            response.put("route", route);
            return Response.ok(response).build();
        } catch (Exception e) {
            log.error("Error finding route with max name: {}", e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Не удалось найти маршрут с максимальным именем", "error_type", RouteErrorType.INTERNAL_ERROR))
                    .build();
        }
    }

    /**
     * Подсчитать количество маршрутов с рейтингом меньше заданного
     */
    @GET
    @Path("/count-rating-less")
    public Response countRoutesWithRatingLessThan(@QueryParam("threshold") Long threshold) {
        try {
            if (threshold == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Параметр threshold обязателен", "error_type", RouteErrorType.INVALID_ARGUMENT))
                        .build();
            }

            long count = routeService.countRoutesWithRatingLessThan(threshold);
            Map<String, Object> response = new HashMap<>();
            response.put("count", count);
            response.put("threshold", threshold);
            return Response.ok(response).build();
        } catch (Exception e) {
            log.error("Error counting routes with rating less than {}: {}", threshold, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Не удалось подсчитать маршруты", "error_type", RouteErrorType.INTERNAL_ERROR))
                    .build();
        }
    }

    /**
     * Найти маршруты с рейтингом больше заданного
     */
    @GET
    @Path("/rating-greater")
    public Response findRoutesWithRatingGreaterThan(@QueryParam("threshold") Long threshold) {
        try {
            if (threshold == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Параметр threshold обязателен", "error_type", RouteErrorType.INVALID_ARGUMENT))
                        .build();
            }

            List<RouteDto> routes = routeService.findRoutesWithRatingGreaterThan(threshold);
            Map<String, Object> response = new HashMap<>();
            response.put("routes", routes);
            response.put("threshold", threshold);
            response.put("count", routes.size());
            return Response.ok(response).build();
        } catch (Exception e) {
            log.error("Error finding routes with rating greater than {}: {}", threshold, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Не удалось найти маршруты", "error_type", RouteErrorType.INTERNAL_ERROR))
                    .build();
        }
    }

    /**
     * Найти маршруты между указанными локациями
     */
    @GET
    @Path("/between-locations")
    public Response findRoutesBetweenLocations(
            @QueryParam("fromLocationName") String fromLocationName,
            @QueryParam("toLocationName") String toLocationName,
            @QueryParam("sortBy") String sortBy) {
        try {
            List<RouteDto> routes = routeService.findRoutesBetweenLocations(fromLocationName, toLocationName, sortBy);
            Map<String, Object> response = new HashMap<>();
            response.put("routes", routes);
            response.put("fromLocationName", fromLocationName);
            response.put("toLocationName", toLocationName);
            response.put("sortBy", sortBy);
            response.put("count", routes.size());
            return Response.ok(response).build();
        } catch (Exception e) {
            log.error("Error finding routes between locations '{}' and '{}': {}", 
                     fromLocationName, toLocationName, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Не удалось найти маршруты между локациями", "error_type", RouteErrorType.INTERNAL_ERROR))
                    .build();
        }
    }

    /**
     * Добавить маршрут между указанными локациями
     * ВАЖНО: Этот эндпоинт использует ту же валидацию уникальности имени, что и основной POST /routes
     */
    @POST
    @Path("/add-between-locations")
    public Response addRouteBetweenLocations(
            @QueryParam("routeName") String routeName,
            @QueryParam("coordX") Double coordX,
            @QueryParam("coordY") Double coordY,
            @QueryParam("fromX") Double fromX,
            @QueryParam("fromY") Double fromY,
            @QueryParam("fromName") String fromName,
            @QueryParam("toX") Double toX,
            @QueryParam("toY") Double toY,
            @QueryParam("toName") String toName,
            @QueryParam("distance") Long distance,
            @QueryParam("rating") Long rating) {
        
        log.info("SPECIAL OPERATIONS: Received request to add route between locations: {}", routeName);
        
        try {
            // Валидация обязательных параметров
            if (routeName == null || routeName.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Имя маршрута обязательно", "error_type", RouteErrorType.INVALID_ARGUMENT))
                        .build();
            }
            
            if (coordX == null || coordY == null || fromX == null || fromY == null || 
                toX == null || toY == null || distance == null || rating == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Все параметры координат, расстояния и рейтинга обязательны", 
                                      "type", RouteErrorType.INVALID_ARGUMENT))
                        .build();
            }

            // Используем сервисный метод, который включает валидацию уникальности имени
            RouteDto created = routeService.addRouteBetweenLocations(
                routeName, coordX, coordY, 
                fromX, fromY, fromName != null ? fromName : "", 
                toX, toY, toName != null ? toName : "", 
                distance, rating
            );
            
            log.info("SPECIAL OPERATIONS: Route created successfully: {}", created.id());
            Map<String, Object> response = new HashMap<>();
            response.put("route", created);
            return Response.status(Response.Status.CREATED).entity(response).build();
            
        } catch (RouteNameAlreadyExistsException e) {
            log.warn("SPECIAL OPERATIONS: Route name already exists: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("error_type", RouteErrorType.DUPLICATE_NAME);
            if (e.getConflictingRoute() != null) {
                response.put("route", e.getConflictingRoute());
                log.info("SPECIAL OPERATIONS: Added conflicting route to response: ID={}, Name='{}'",
                        e.getConflictingRoute().id(), e.getConflictingRoute().name());
            }
            return Response.status(Response.Status.CONFLICT)
                    .entity(response)
                    .build();
        } catch (RouteZeroDistanceException e) {
            log.warn("SPECIAL OPERATIONS: Zero distance route validation failed: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("error_type", RouteErrorType.ZERO_DISTANCE_ROUTE);
            response.put("fromX", e.getFromX());
            response.put("fromY", e.getFromY());
            response.put("toX", e.getToX());
            response.put("toY", e.getToY());
            return Response.status(Response.Status.CONFLICT)
                    .entity(response)
                    .build();
        } catch (ValidationException e) {
            log.warn("SPECIAL OPERATIONS: Validation error: {}", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage(), "error_type", RouteErrorType.VALIDATION_ERROR))
                    .build();
        } catch (IllegalArgumentException e) {
            log.warn("SPECIAL OPERATIONS: Invalid argument: {}", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage(), "error_type", RouteErrorType.INVALID_ARGUMENT))
                    .build();
        } catch (Exception e) {
            log.error("SPECIAL OPERATIONS: Unexpected error: {}", e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Не удалось создать маршрут между локациями", "error_type", RouteErrorType.INTERNAL_ERROR))
                    .build();
        }
    }
}