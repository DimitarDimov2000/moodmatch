package com.moodmatch.service;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class CdiSecurityIdentityProvider implements SecurityIdentityProvider {

    @Inject
    SecurityIdentity securityIdentity;

    @Override
    public SecurityIdentity getSecurityIdentity() {
        return securityIdentity;
    }
}
