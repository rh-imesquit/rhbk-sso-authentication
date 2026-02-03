package com.redhat.resource;

import com.redhat.service.SettlementService;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("/automation/settlement")
public class SettlementResource {

    @Inject
    SettlementService settlementService;

    @POST
    @Path("/trigger")
    public Response triggerProcess() {
        settlementService.runSettlementProcess();
        return Response.accepted().entity("Settlement process triggered.").build();
    }
}