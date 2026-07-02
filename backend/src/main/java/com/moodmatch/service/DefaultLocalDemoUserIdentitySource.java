package com.moodmatch.service;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DefaultLocalDemoUserIdentitySource implements LocalDemoUserIdentitySource {

    @Override
    public LocalDemoUserIdentity getIdentity() {
        return new LocalDemoUserIdentity(
                LocalDemoCurrentUserProvider.LOCAL_DEMO_USER_ID,
                LocalDemoCurrentUserProvider.LOCAL_DEMO_PROVIDER,
                LocalDemoCurrentUserProvider.LOCAL_DEMO_PROVIDER_SUBJECT,
                LocalDemoCurrentUserProvider.LOCAL_DEMO_EMAIL,
                LocalDemoCurrentUserProvider.LOCAL_DEMO_DISPLAY_NAME);
    }
}
