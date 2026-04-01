package com.redhat.service;

import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.client.Tokens;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class TokenService {

    @Inject
    OidcClient oidcClient;

    public String getAccessToken() {
        Tokens tokens = oidcClient.getTokens().await().indefinitely();
        return tokens.getAccessToken();
    }
}
