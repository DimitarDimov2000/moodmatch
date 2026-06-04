# Testing Strategy

MoodMatch will be considered complete only when deterministic scoring, filtering, edge cases, and status transitions are covered by tests.

The quality strategy will focus on keeping rule-based behavior explicit, repeatable, and explainable before implementation is considered complete.

## Planned Test Coverage

### Unit Tests

Unit tests will cover profile calculation to verify that consumed and positively weighted media items produce the expected interest profile.

Unit tests will cover matching score calculation to verify deterministic score output, tie handling, weighting behavior, and explanation details.

Unit tests will cover Decision Mode to verify filtering rules, candidate narrowing, and edge cases where no candidate matches.

### Validation Tests

Validation tests will cover status rules and status transitions, including valid transitions, invalid transitions, and field-level constraints.

### REST API Tests

REST API tests will use RestAssured for backend endpoints. These tests will verify request and response shapes, validation behavior, status codes, and DTO boundaries.

### Frontend Tests

Frontend tests with Vitest are optional for the initial architecture, but planned for UI behavior that contains meaningful conditional rendering, filtering controls, or data transformation.

## Completion Expectations

Before implementation is considered complete, the project must include tests for:

| Area | Required focus |
| --- | --- |
| Scoring | Deterministic scores, explainable outputs, weighting, and ties. |
| Filtering | Decision Mode filters and candidate narrowing. |
| Edge cases | Empty profiles, missing tags, unknown commitment levels, and no-match scenarios. |
| Status transitions | Valid and invalid status changes. |
