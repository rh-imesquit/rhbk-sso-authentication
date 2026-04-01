package com.redhat.client;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.quarkus.oidc.client.filter.OidcClientFilter;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/")
@RegisterRestClient(configKey = "cbv-api")
@OidcClientFilter
public interface CBVApiClient {

    @GET
    @Path("/cbv/001")
    @Produces(MediaType.TEXT_PLAIN)
    String callCbv001();

    @GET
    @Path("/cbv/002")
    @Produces(MediaType.TEXT_PLAIN)
    String callCbv002();
}
