package com.redhat.resource;

import com.redhat.service.CBVGatewayService;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/test")
public class TestCBVResource {

    @Inject
    CBVGatewayService service;

    @GET
    @Path("/001")
    @Produces(MediaType.TEXT_PLAIN)
    public String test001() {
        return service.test001();
    }

    @GET
    @Path("/002")
    @Produces(MediaType.TEXT_PLAIN)
    public String test002() {
        return service.test002();
    }
}