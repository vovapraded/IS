package org.example.domain.coordinates.controller;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.example.domain.coordinates.dto.CoordinatesDto;
import org.example.domain.coordinates.service.CoordinatesServiceMB;

import java.util.List;

@Path("/coordinates")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CoordinatesResource {

    @Inject
    private CoordinatesServiceMB coordinatesService;

    @GET
    public List<CoordinatesDto> getAllCoordinates() {
        return coordinatesService.findAll();
    }

    @GET
    @Path("/search")
    public List<CoordinatesDto> searchCoordinates(
            @QueryParam("x") Float x,
            @QueryParam("y") Double y) {
        
        if (x != null && y != null) {
            return coordinatesService.findByExample(x, y);
        } else {
            return coordinatesService.findAll();
        }
    }

    @GET
    @Path("/exists")
    public boolean coordinatesExist(
            @QueryParam("x") @DefaultValue("0") float x,
            @QueryParam("y") @DefaultValue("0") Double y) {
        
        return coordinatesService.exists(x, y);
    }

    @GET
    @Path("/usage-count")
    public long getUsageCount(
            @QueryParam("x") @DefaultValue("0") float x,
            @QueryParam("y") @DefaultValue("0") Double y) {
        
        return coordinatesService.getUsageCount(x, y);
    }
}