package org.example.controller;

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
import org.example.dto.*;
import org.example.service.RouteServiceMB;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/routes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RouteResource {

    @Inject
    private RouteServiceMB routeService;

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
        
        List<RouteDto> routes = routeService.findAll(page, size, nameFilter, sortBy, sortDirection);
        long totalElements = routeService.countWithFilter(nameFilter);
        int totalPages = (int) Math.ceil((double) totalElements / size);
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", routes);
        response.put("totalElements", totalElements);
        response.put("totalPages", totalPages);
        response.put("currentPage", page);
        response.put("size", size);
        
        return Response.ok(response).build();
    }

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
        // На всякий случай сверим id в path и в теле
        if (!dto.id().equals(id)) {
            throw new BadRequestException("Path ID and body ID must match");
        }
        return routeService.updateRoute(dto);
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Integer id) {
        routeService.delete(id);
        return Response.noContent().build();
    }

    // Специальные операции
    @GET
    @Path("/special/max-name")
    public RouteDto getRouteWithMaxName() {
        return routeService.findRouteWithMaxName();
    }

    @GET
    @Path("/special/count-rating-less-than/{rating}")
    public Response countRoutesWithRatingLessThan(@PathParam("rating") Long rating) {
        long count = routeService.countRoutesWithRatingLessThan(rating);
        Map<String, Object> response = new HashMap<>();
        response.put("count", count);
        response.put("threshold", rating);
        return Response.ok(response).build();
    }

    @GET
    @Path("/special/rating-greater-than/{rating}")
    public List<RouteDto> getRoutesWithRatingGreaterThan(@PathParam("rating") Long rating) {
        return routeService.findRoutesWithRatingGreaterThan(rating);
    }

    @GET
    @Path("/special/between-locations")
    public List<RouteDto> findRoutesBetweenLocations(
            @QueryParam("from") String fromLocationName,
            @QueryParam("to") String toLocationName,
            @QueryParam("sortBy") @DefaultValue("name") String sortBy) {
        return routeService.findRoutesBetweenLocations(fromLocationName, toLocationName, sortBy);
    }

    @POST
    @Path("/special/add-between-locations")
    public Response addRouteBetweenLocations(Map<String, Object> requestData) {
        try {
            String routeName = (String) requestData.get("routeName");
            float coordX = ((Number) requestData.get("coordX")).floatValue();
            Double coordY = ((Number) requestData.get("coordY")).doubleValue();
            Double fromX = ((Number) requestData.get("fromX")).doubleValue();
            double fromY = ((Number) requestData.get("fromY")).doubleValue();
            String fromName = (String) requestData.get("fromName");
            Double toX = ((Number) requestData.get("toX")).doubleValue();
            double toY = ((Number) requestData.get("toY")).doubleValue();
            String toName = (String) requestData.get("toName");
            Long distance = ((Number) requestData.get("distance")).longValue();
            Long rating = ((Number) requestData.get("rating")).longValue();

            RouteDto createdRoute = routeService.addRouteBetweenLocations(
                    routeName, coordX, coordY, fromX, fromY, fromName,
                    toX, toY, toName, distance, rating);

            return Response.status(Response.Status.CREATED).entity(createdRoute).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Invalid request data: " + e.getMessage()))
                    .build();
        }
    }
}
