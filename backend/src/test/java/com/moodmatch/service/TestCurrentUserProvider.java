package com.moodmatch.service;

public final class TestCurrentUserProvider {

    private TestCurrentUserProvider() {}

    public static void useLocalDemoUser() {
        TestLocalDemoUserIdentitySource.useLocalDemoUser();
    }

    public static void useUserA() {
        TestLocalDemoUserIdentitySource.useUserA();
    }

    public static void useUserB() {
        TestLocalDemoUserIdentitySource.useUserB();
    }
}
