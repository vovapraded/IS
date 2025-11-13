package org.example.domain.location.controller;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.example.domain.location.dto.LocationDto;
import org.example.domain.location.service.LocationServiceMB;

import java.util.List;

@Path("/locations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LocationResource {

    @Inject
    private LocationServiceMB locationService;

    @GET
    public List<LocationDto> getAllLocations() {
        return locationService.findAll();
    }

    @GET
    @Path("/from")
    public List<LocationDto> getAllFromLocations() {
        return locationService.findAllFrom();
    }

    @GET
    @Path("/to")
    public List<LocationDto> getAllToLocations() {
        return locationService.findAllTo();
    }

    @GET
    @Path("/names")
    public List<String> getDistinctNames() {
        return locationService.findDistinctNames();
    }

    @GET
    @Path("/search")
    public List<LocationDto> searchLocations(
            @QueryParam("x") Double x,
            @QueryParam("y") Double y,
            @QueryParam("name") String name) {
        
        return locationService.findByExample(x, y, name);
    }

    @GET
    @Path("/search-by-name")
    public List<LocationDto> searchByName(@QueryParam("name") String namePattern) {
        return locationService.searchByName(namePattern);
    }

    @GET
    @Path("/exists")
    public boolean locationExists(
            @QueryParam("x") Double x,
            @QueryParam("y") @DefaultValue("0") double y,
            @QueryParam("name") String name) {
        
        return locationService.exists(x, y, name);
    }

    @GET
    @Path("/usage-count")
    public long getUsageCount(
            @QueryParam("x") Double x,
            @QueryParam("y") @DefaultValue("0") double y,
            @QueryParam("name") String name) {
        
        return locationService.getUsageCount(x, y, name);
    }
}