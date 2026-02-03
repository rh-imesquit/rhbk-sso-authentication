package com.redhat.resource;

import com.redhat.model.Transaction;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.ArrayList;

@Path("/v1/transactions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TransactionResource {

    @GET
    @Path("/pending")
    @RolesAllowed("view-transactions") // Role definida no Keycloak para este client
    public List<Transaction> getPendingTransactions() {
        List<Transaction> list = new ArrayList<>();
        list.add(new Transaction(1L, 1500.0, "BRL", "PENDING"));
        list.add(new Transaction(2L, 3200.50, "BRL", "PENDING"));
        return list;
    }

    @POST
    @Path("/{id}/reconcile")
    @RolesAllowed("execute-reconciliation") // Role definida no Keycloak
    public Response reconcile(@PathParam("id") Long id) {
        // Business logic to reconcile
        System.out.println("Transaction " + id + " reconciled by Service Account.");
        return Response.ok().build();
    }
}