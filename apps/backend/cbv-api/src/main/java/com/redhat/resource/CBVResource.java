package com.redhat.resource;

import com.redhat.config.SecurityRoles;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/cbv")
public class CBVResource {

    @GET
    @Path("/001")
    @RolesAllowed(SecurityRoles.CBV_001)
    @Produces(MediaType.TEXT_PLAIN)
    public String cbv001() {
        return "Authorized access to TRANCODE CBV_001";
    }

    @GET
    @Path("/002")
    @RolesAllowed(SecurityRoles.CBV_002)
    @Produces(MediaType.TEXT_PLAIN)
    public String cbv002() {
        return "Authorized access to TRANCODE CBV_002";
    }
}