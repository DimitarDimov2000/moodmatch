package com.moodmatch.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.moodmatch.dto.tag.TagResponse;
import com.moodmatch.entity.CommitmentLevel;
import com.moodmatch.entity.ConsumptionStatus;
import com.moodmatch.entity.TagCategory;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class ProfileResourceTest extends MatchingResourceTestSupport {

    @Test
    void shouldReturnCalculatedProfileDto() {
        TagResponse scienceFiction = createTag("Science-Fiction", TagCategory.GENRE);
        TagResponse time = createTag("Zeit", TagCategory.THEME);
        TagResponse suspense = createTag("spannend", TagCategory.TONE);

        createMediaWithTags(
                "Interstellar", ConsumptionStatus.CONSUMED, 5, true, CommitmentLevel.LONG, List.of(scienceFiction.id(), time.id()));
        createMediaWithTags(
                "Dark", ConsumptionStatus.CONSUMED, 4, false, CommitmentLevel.LONG, List.of(time.id(), suspense.id()));
        createMediaWithTags(
                "Arrival", ConsumptionStatus.CONSUMED, 5, false, CommitmentLevel.MEDIUM, List.of(suspense.id()));

        given()
                .when()
                .get("/api/profile")
                .then()
                .statusCode(200)
                .body("isReadyForMatching", is(true))
                .body("profileRelevantMediaCount", is(3))
                .body("requiredProfileRelevantMediaCount", is(3))
                .body("contributingMedia", hasSize(3))
                .body("weightedTags", hasSize(3))
                .body("weightedTags[0].tag.name", is("Zeit"))
                .body("weightedTags[0].weight", is(10.35f))
                .body("weightedTags[1].tag.name", is("spannend"))
                .body("weightedTags[1].weight", is(5.40f))
                .body("weightedTags[2].tag.name", is("Science-Fiction"))
                .body("weightedTags[2].weight", is(2.25f));
    }

    @Test
    void shouldExposeSuppressedProfileStateWhenInsufficientDataExists() {
        TagResponse identity = createTag("Identität", TagCategory.THEME);

        createMediaWithTags(
                "Severance", ConsumptionStatus.CONSUMED, 5, false, CommitmentLevel.MEDIUM, List.of(identity.id()));
        createMediaWithTags(
                "Dark Matter", ConsumptionStatus.CONSUMED, 4, false, CommitmentLevel.MEDIUM, List.of(identity.id()));
        createMediaWithTags(
                "Wishlist", ConsumptionStatus.WANT_TO_CONSUME, null, false, CommitmentLevel.SHORT, List.of(identity.id()));

        given()
                .when()
                .get("/api/profile")
                .then()
                .statusCode(200)
                .body("isReadyForMatching", is(false))
                .body("profileRelevantMediaCount", is(2))
                .body("requiredProfileRelevantMediaCount", is(3))
                .body("weightedTags", hasSize(1))
                .body("explanationMessage", is(
                        "Scores are suppressed until at least 3 consumed items rated 4 or 5 with confirmed tags are available. Current count: 2."));
    }
}
