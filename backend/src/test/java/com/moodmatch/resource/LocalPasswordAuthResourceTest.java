package com.moodmatch.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.moodmatch.testsupport.LocalPasswordAuthTestProfile;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@QuarkusTest
@TestProfile(LocalPasswordAuthTestProfile.class)
class LocalPasswordAuthResourceTest {

    @Inject
    EntityManager entityManager;

    @BeforeEach
    void cleanDatabaseBefore() {
        cleanDatabase();
    }

    @AfterEach
    void cleanDatabaseAfter() {
        cleanDatabase();
    }

    @Test
    void shouldRegisterLoginAndReturnCurrentUserWithoutPasswordHash() {
        String email = uniqueEmail("register");

        Response registerResponse = given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "email", email.toUpperCase(),
                        "password", "password123",
                        "displayName", "Mood Student"))
                .when()
                .post("/api/auth/register");

        registerResponse.then()
                .statusCode(200)
                .body("token", notNullValue())
                .body("user.id", notNullValue())
                .body("user.email", is(email))
                .body("user.displayName", is("Mood Student"))
                .body("user.passwordHash", nullValue());

        String token = registerResponse.jsonPath().getString("token");

        given()
                .header("Authorization", bearer(token))
                .when()
                .get("/api/auth/me")
                .then()
                .statusCode(200)
                .body("email", is(email))
                .body("displayName", is("Mood Student"))
                .body("passwordHash", nullValue());

        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "email", email,
                        "password", "password123"))
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(200)
                .body("token", notNullValue())
                .body("user.email", is(email));
    }

    @Test
    void shouldRejectDuplicateEmailWithConflict() {
        String email = uniqueEmail("duplicate");
        register(email, "password123", "First User");

        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "email", email.toUpperCase(),
                        "password", "password123",
                        "displayName", "Second User"))
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(409)
                .body("message", is("Email is already registered."));
    }

    @Test
    void shouldRejectWrongPasswordWithoutRevealingAccountState() {
        String email = uniqueEmail("wrong-password");
        register(email, "password123", "Login User");

        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "email", email,
                        "password", "incorrect123"))
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(401)
                .body("message", is("Invalid email or password."));
    }

    @Test
    void shouldProtectPrivateEndpointsWithBearerToken() {
        given()
                .when()
                .get("/api/media")
                .then()
                .statusCode(401);

        given()
                .header("Authorization", bearer("not-a-valid-token"))
                .when()
                .get("/api/media")
                .then()
                .statusCode(401);

        String token = register(uniqueEmail("protected"), "password123", "Protected User");

        given()
                .header("Authorization", bearer(token))
                .when()
                .get("/api/media")
                .then()
                .statusCode(200)
                .body("$", empty());
    }

    @Test
    void shouldScopeMediaToTheAuthenticatedUser() {
        String userAToken = register(uniqueEmail("owner-a"), "password123", "Owner A");
        String userBToken = register(uniqueEmail("owner-b"), "password123", "Owner B");
        String title = "Private Media " + UUID.randomUUID();

        given()
                .header("Authorization", bearer(userAToken))
                .contentType(ContentType.JSON)
                .body(buildMediaPayload(title))
                .when()
                .post("/api/media")
                .then()
                .statusCode(201)
                .body("title", is(title));

        given()
                .header("Authorization", bearer(userAToken))
                .when()
                .get("/api/media")
                .then()
                .statusCode(200)
                .body("title", hasItem(title));

        given()
                .header("Authorization", bearer(userBToken))
                .when()
                .get("/api/media")
                .then()
                .statusCode(200)
                .body("title", not(hasItem(title)));
    }

    @Test
    void shouldRevokeTokenOnLogout() {
        String token = register(uniqueEmail("logout"), "password123", "Logout User");

        given()
                .header("Authorization", bearer(token))
                .when()
                .post("/api/auth/logout")
                .then()
                .statusCode(204);

        given()
                .header("Authorization", bearer(token))
                .when()
                .get("/api/auth/me")
                .then()
                .statusCode(401);
    }

    private String register(String email, String password, String displayName) {
        return given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "email", email,
                        "password", password,
                        "displayName", displayName))
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getString("token");
    }

    private Map<String, Object> buildMediaPayload(String title) {
        return Map.of(
                "title", title,
                "mediaType", "FILM",
                "consumptionStatus", "CONSUMED",
                "isFavourite", false,
                "rating", 5,
                "sourceType", "MANUAL",
                "commitmentLevel", "MEDIUM",
                "metadataOrigin", "MANUAL");
    }

    private String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private void cleanDatabase() {
        QuarkusTransaction.requiringNew().run(() -> {
            entityManager.createNativeQuery("DELETE FROM media_tags").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM media_external_refs").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM media_items").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM external_tag_mappings").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM tags").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM auth_sessions").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM app_users").executeUpdate();
        });
    }
}
