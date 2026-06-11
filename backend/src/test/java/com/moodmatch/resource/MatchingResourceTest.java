package com.moodmatch.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.moodmatch.dto.tag.TagResponse;
import com.moodmatch.entity.CommitmentLevel;
import com.moodmatch.entity.ConsumptionStatus;
import com.moodmatch.entity.TagCategory;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class MatchingResourceTest extends MatchingResourceTestSupport {

    @Test
    void shouldReturnDeterministicMatchResults() {
        TagResponse scienceFiction = createTag("Science-Fiction", TagCategory.GENRE);
        TagResponse time = createTag("Zeit", TagCategory.THEME);
        TagResponse suspense = createTag("spannend", TagCategory.TONE);
        TagResponse mystery = createTag("Mystery", TagCategory.GENRE);
        TagResponse comedy = createTag("Comedy", TagCategory.GENRE);

        createMediaWithTags(
                "Interstellar", ConsumptionStatus.CONSUMED, 5, true, CommitmentLevel.LONG, List.of(scienceFiction.id(), time.id()));
        createMediaWithTags(
                "Dark", ConsumptionStatus.CONSUMED, 4, false, CommitmentLevel.LONG, List.of(time.id(), suspense.id()));
        createMediaWithTags(
                "Arrival", ConsumptionStatus.CONSUMED, 5, false, CommitmentLevel.MEDIUM, List.of(suspense.id()));

        createMediaWithTags(
                "The Expanse", ConsumptionStatus.WANT_TO_CONSUME, null, false, CommitmentLevel.LONG, List.of(time.id(), suspense.id()));
        createMediaWithTags(
                "Severance", ConsumptionStatus.WANT_TO_CONSUME, null, false, CommitmentLevel.MEDIUM, List.of(time.id(), mystery.id()));
        createMediaWithTags(
                "Ted Lasso", ConsumptionStatus.WANT_TO_CONSUME, null, false, CommitmentLevel.SHORT, List.of(comedy.id()));

        given()
                .when()
                .get("/api/matches")
                .then()
                .statusCode(200)
                .body("scoresSuppressed", is(false))
                .body("matches", hasSize(3))
                .body("matches[0].candidate.media.title", is("The Expanse"))
                .body("matches[0].rawScore", is(15.75f))
                .body("matches[0].precisionFactor", is(1.0f))
                .body("matches[0].adjustedScore", is(15.75f))
                .body("matches[0].relativeScore", is(100.0f))
                .body("matches[1].candidate.media.title", is("Severance"))
                .body("matches[1].adjustedScore", is(5.18f))
                .body("matches[1].relativeScore", is(32.86f))
                .body("matches[2].candidate.media.title", is("Ted Lasso"))
                .body("matches[2].adjustedScore", is(0.0f))
                .body("matches[2].relativeScore", nullValue())
                .body("matches[2].explanationMessage", is(
                        "This candidate has confirmed tags, but none overlap with your current interest profile."));
    }

    @Test
    void shouldSuppressScoresWhenProfileDataIsInsufficient() {
        TagResponse identity = createTag("Identität", TagCategory.THEME);

        createMediaWithTags(
                "Dark Matter", ConsumptionStatus.CONSUMED, 5, false, CommitmentLevel.MEDIUM, List.of(identity.id()));
        createMediaWithTags(
                "Silo", ConsumptionStatus.CONSUMED, 4, false, CommitmentLevel.MEDIUM, List.of(identity.id()));
        createMediaWithTags(
                "The Leftovers", ConsumptionStatus.WANT_TO_CONSUME, null, false, CommitmentLevel.LONG, List.of(identity.id()));

        given()
                .when()
                .get("/api/matches")
                .then()
                .statusCode(200)
                .body("scoresSuppressed", is(true))
                .body("matches", hasSize(1))
                .body("matches[0].candidate.media.title", is("The Leftovers"))
                .body("matches[0].rawScore", nullValue())
                .body("matches[0].precisionFactor", nullValue())
                .body("matches[0].adjustedScore", nullValue())
                .body("matches[0].relativeScore", nullValue())
                .body("explanationMessage", is(
                        "Scores are suppressed until at least 3 consumed items rated 4 or 5 with confirmed tags are available. Current count: 2."));
    }

    @Test
    void shouldDistinguishNoOverlapFromIncompleteCandidates() {
        TagResponse survival = createTag("Überleben", TagCategory.THEME);
        TagResponse intense = createTag("intensiv", TagCategory.TONE);
        TagResponse comedy = createTag("Comedy", TagCategory.GENRE);

        createMediaWithTags(
                "Dune", ConsumptionStatus.CONSUMED, 5, false, CommitmentLevel.LONG, List.of(survival.id()));
        createMediaWithTags(
                "Children of Men", ConsumptionStatus.CONSUMED, 5, false, CommitmentLevel.LONG, List.of(survival.id(), intense.id()));
        createMediaWithTags(
                "The Road", ConsumptionStatus.CONSUMED, 4, false, CommitmentLevel.MEDIUM, List.of(survival.id()));

        createMediaWithTags(
                "Silo", ConsumptionStatus.WANT_TO_CONSUME, null, false, CommitmentLevel.MEDIUM, List.of(survival.id()));
        createMediaWithTags(
                "Parks and Recreation", ConsumptionStatus.WANT_TO_CONSUME, null, false, CommitmentLevel.SHORT, List.of(comedy.id()));
        createMediaWithTags(
                "Untitled Wishlist", ConsumptionStatus.WANT_TO_CONSUME, null, false, CommitmentLevel.UNKNOWN, List.of());

        given()
                .when()
                .get("/api/matches")
                .then()
                .statusCode(200)
                .body("scoresSuppressed", is(false))
                .body("explanationMessage", is(
                        "Relative percentages are unavailable until at least two candidates have a positive adjusted score."))
                .body("matches[0].candidate.media.title", is("Silo"))
                .body("matches[0].relativeScore", nullValue())
                .body("matches[1].candidate.media.title", is("Parks and Recreation"))
                .body("matches[1].candidate.isCompleteForMatching", is(true))
                .body("matches[1].adjustedScore", is(0.0f))
                .body("matches[1].explanationMessage", is(
                        "This candidate has confirmed tags, but none overlap with your current interest profile."))
                .body("matches[2].candidate.media.title", is("Untitled Wishlist"))
                .body("matches[2].candidate.isCompleteForMatching", is(false))
                .body("matches[2].adjustedScore", is(0.0f))
                .body("matches[2].explanationMessage", is(
                        "This candidate cannot be matched yet because it has no confirmed candidate tags."));
    }
}
