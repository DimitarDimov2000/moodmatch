package com.moodmatch.service;

import io.quarkus.security.identity.SecurityIdentity;

public interface SecurityIdentityProvider {

    SecurityIdentity getSecurityIdentity();
}
