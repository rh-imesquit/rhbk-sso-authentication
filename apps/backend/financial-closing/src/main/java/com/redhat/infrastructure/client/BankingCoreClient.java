package com.redhat.infrastructure.client;

import com.redhat.model.TransactionDTO;
import io.quarkus.oidc.client.filter.OidcClientFilter;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/v1/transactions")
@RegisterRestClient(configKey = "banking-core-api")
@OidcClientFilter("settlement-service-client") // Links to the OIDC configuration
public interface BankingCoreClient {

    @GET
    @Path("/pending")
    @Produces(MediaType.APPLICATION_JSON)
    List<TransactionDTO> getPendingTransactions();

    @POST
    @Path("/{id}/reconcile")
    @Consumes(MediaType.APPLICATION_JSON)
    void reconcileTransaction(@PathParam("id") Long id);
}