package org.example.domain.route.controller;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.route.dto.RouteDto;
import org.example.domain.route.dto.RouteErrorType;
import org.example.domain.route.service.RouteServiceMB;
import org.example.domain.location.dto.LocationDto;
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
    @Path("/count-rating-less-than/{threshold}")
    public Response countRoutesWithRatingLessThan(@PathParam("threshold") Long threshold) {
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
    @Path("/rating-greater-than/{threshold}")
    public Response findRoutesWithRatingGreaterThan(@PathParam("threshold") Long threshold) {
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
            @QueryParam("from") String fromLocationName,
            @QueryParam("to") String toLocationName,
            @QueryParam("sortBy") String sortBy) {
        try {
            List<RouteDto> routes = routeService.findRoutesBetweenLocations(fromLocationName, toLocationName, sortBy);
            return Response.ok(routes).build();
        } catch (Exception e) {
            log.error("Error finding routes between locations '{}' and '{}': {}",
                     fromLocationName, toLocationName, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Не удалось найти маршруты между локациями", "error_type", RouteErrorType.INTERNAL_ERROR))
                    .build();
        }
    }

    /**
     * Получить все локации для автокомплита
     */
    @GET
    @Path("/all-locations")
    public Response getAllLocations() {
        try {
            List<LocationDto> locations = routeService.getAvailableLocations();
            return Response.ok(locations).build();
        } catch (Exception e) {
            log.error("Error loading locations: {}", e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to load locations", "error_type", RouteErrorType.INTERNAL_ERROR))
                    .build();
        }
    }

}