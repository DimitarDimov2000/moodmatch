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
class CandidateResourceTest extends MatchingResourceTestSupport {

    @Test
    void shouldReturnOnlyWantToConsumeCandidatesWithMatchingCompletenessFlags() {
        TagResponse theme = createTag("Überleben", TagCategory.THEME);
        TagResponse tone = createTag("intensiv", TagCategory.TONE);

        createMediaWithTags(
                "Dune", ConsumptionStatus.CONSUMED, 5, false, CommitmentLevel.LONG, List.of(theme.id()));
        createMediaWithTags(
                "Silo", ConsumptionStatus.WANT_TO_CONSUME, null, false, CommitmentLevel.MEDIUM, List.of(theme.id(), tone.id()));
        createMediaWithTags(
                "Untitled Wishlist", ConsumptionStatus.WANT_TO_CONSUME, null, false, CommitmentLevel.UNKNOWN, List.of());
        createMediaWithTags(
                "Rejected", ConsumptionStatus.NOT_INTERESTED, null, false, CommitmentLevel.SHORT, List.of(theme.id()));

        given()
                .when()
                .get("/api/candidates")
                .then()
                .statusCode(200)
                .body("candidates", hasSize(2))
                .body("candidates[0].media.title", is("Silo"))
                .body("candidates[0].isCompleteForMatching", is(true))
                .body("candidates[0].media.tags", hasSize(2))
                .body("candidates[1].media.title", is("Untitled Wishlist"))
                .body("candidates[1].isCompleteForMatching", is(false))
                .body("candidates[1].media.tags", hasSize(0));
    }
}
