# Scoring And Matching

This document describes the current implemented scoring and matching behavior in MoodMatch. It is based on the backend services and reflects the code as it exists today.

Primary sources:

- `backend/src/main/java/com/moodmatch/service/InterestProfileService.java`
- `backend/src/main/java/com/moodmatch/service/MatchingService.java`
- `backend/src/main/java/com/moodmatch/service/CandidateService.java`

## Profile-Relevant Media

A media item contributes to the interest profile only if all of the following are true:

- `consumptionStatus = CONSUMED`
- `rating` is `4` or `5`
- the item has at least one confirmed local tag

Media that do not meet all three conditions are ignored for profile building.

## Rating Weights

- rating `4` -> `1.2`
- rating `5` -> `1.5`

Other ratings are not profile-relevant.

## Favourite Factor

- favourite -> `1.5`
- non-favourite -> `1.0`

This factor applies only after a media item is already profile-relevant.

## Tag Category Weights

MoodMatch uses the current `TagCategory` values from the backend:

- `THEME` -> `3.0`
- `EXPERIENCE` -> `2.5`
- `TONE` -> `2.0`
- `SETTING` -> `1.5`
- `GENRE` -> `1.0`

## Profile Weight Accumulation

For each tag on each profile-relevant media item:

```text
tag contribution = rating weight x favourite factor x category weight
```

MoodMatch then accumulates contributions by tag id:

```text
profile weight for a tag = sum of all contributions for that tag
```

The backend returns these accumulated weights as the current interest profile.

## Candidate Definition

A candidate is any media item with:

- `consumptionStatus = WANT_TO_CONSUME`

## Candidate Completeness

Candidates are considered complete for matching only when they have at least one confirmed local tag.

If a candidate has no confirmed local tags:

- it still appears as a candidate
- it is marked incomplete for matching
- it does not receive a meaningful score

## Matching Pipeline

The implemented matching pipeline is:

1. Build the interest profile from profile-relevant media.
2. Load candidates from `WANT_TO_CONSUME` media.
3. For each complete candidate, compare its confirmed tags to the profile weights.
4. Calculate raw score.
5. Calculate precision factor.
6. Calculate adjusted score.
7. Determine whether the candidate is meaningful.
8. Calculate relative score only when comparison is meaningful.

### Raw Score

```text
raw score = sum of profile weights for overlapping candidate tags
```

Only candidate tags that already exist in the current profile contribute to the raw score.

### Precision Factor

```text
precision factor = matching tag count / candidate tag count
```

If `candidate tag count = 0`, the precision factor is `0`.

### Adjusted Score

```text
adjusted score = raw score x precision factor
```

This penalizes over-tagged candidates that only partially overlap with the profile.

### Meaningful Candidate

A candidate is meaningful only when:

- it has at least one confirmed tag
- its adjusted score is greater than `0`

### Relative Score

Relative score is only returned when both of these are true:

- at least two meaningful candidates exist
- the maximum adjusted score among meaningful candidates is greater than `0`

If that comparison is not meaningful, `relativeScore` is returned as `null`.

## Sorting

Candidates are sorted by:

1. adjusted score descending
2. `createdAt` ascending
3. `id` ascending

## Score Suppression And Edge Cases

### Fewer Than 3 Profile-Relevant Media

If fewer than 3 profile-relevant media items exist:

- the profile is marked not ready
- scores are suppressed
- match results return explanation text instead of usable scores

### Incomplete Candidate

If a candidate has no confirmed local tags:

- it is incomplete for matching
- raw score, precision factor, adjusted score, and relative score are not meaningful
- the explanation states that confirmed candidate tags are missing

### No Overlap

If a candidate has confirmed local tags but none overlap with the current profile:

- raw score becomes `0`
- adjusted score becomes `0`
- relative score is not meaningful
- the explanation states that the candidate has confirmed tags but no overlap

### Fewer Than 2 Meaningful Candidates

If fewer than 2 meaningful candidates exist:

- relative percentages are unavailable
- `relativeScore` stays `null`
- the explanation states that comparison is not meaningful yet

## Important Semantics

- Candidate tags are user-estimated expectations until the item is later consumed and rated.
- Swipe behavior does not change scoring directly.
- External preview results do not affect scoring directly.
- Only confirmed local tags and persisted local media state affect the profile and match results.

## Why This Matters For Reviewers

MoodMatch scoring is intentionally deterministic and inspectable:

- profile weights come from persisted consumed media
- candidate comparison uses confirmed local tags only
- percentage scores are withheld when the comparison would be misleading

That is why the app can explain both scored results and unscored edge cases without falling back to opaque ranking behavior.
