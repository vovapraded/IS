package org.example.domain.import_history.controller;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/test-import")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TestImportResource {

    @GET
    @Path("/hello")
    public Response hello() {
        return Response.ok("{\"message\":\"Import endpoints work!\"}").build();
    }

    @OPTIONS
    @Path("/hello")
    public Response helloOptions() {
        return Response.ok().build();
    }
}