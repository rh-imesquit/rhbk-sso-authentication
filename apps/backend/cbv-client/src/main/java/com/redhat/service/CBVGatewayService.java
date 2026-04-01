package com.redhat.service;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.ClientWebApplicationException;

import com.redhat.client.CBVApiClient;
import com.redhat.exception.CBVApiException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class CBVGatewayService {

    @Inject
    @RestClient
    CBVApiClient client;

    @Inject
    TokenService tokenService;

    public String test001() {
        printToken();
        return client.callCbv001();
    }

    public String test002() {
        try {
            printToken();
            return client.callCbv002();
        } catch (ClientWebApplicationException e) {
            throw new CBVApiException(
                    e.getResponse().getStatus(),
                    "Error returned by cbv-api.",
                    "/cbv/002"
            );
        }
    }

    private void printToken() {
        String token = tokenService.getAccessToken();
        System.out.println("Access token: " + token);
    }

}