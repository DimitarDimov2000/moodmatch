# Architecture

MoodMatch is planned as a small monorepo application with a three-layer architecture:

1. A Vue 3 and TypeScript frontend.
2. A Quarkus and Java backend.
3. A PostgreSQL database.

The frontend will communicate with the backend through REST endpoints using JSON request and response bodies. The backend will expose DTOs for API communication instead of exposing database entities directly.

MoodMatch is deterministic, explainable, locally closed, and rule-based. Matching will be calculated from the user's own stored media data and tags. The system will not use artificial intelligence, machine learning, collaborative filtering, or external recommendation APIs.

## Layers

### Frontend

The Vue 3 frontend will provide the user interface for managing media, tags, profile insights, candidates, matching results, decision mode, and dashboard summaries. It will call the backend REST API and render explanations returned by the backend.

### Backend

The Quarkus backend will own validation, persistence boundaries, DTO mapping, profile calculation, matching score calculation, filtering rules, status transitions, and explanation generation. It will keep business rules explicit and testable.

### Database

PostgreSQL will store media items, tags, and media-to-tag relationships. The schema will support a locally closed dataset owned by the user.

## Planned Components

### Media Component

Manages media item lifecycle, including creation, updates, deletion, status changes, favorites, and retrieval.

### Tag Component

Manages available tags and their relationship to media items.

### Profile Component

Calculates an interest profile from consumed and positively weighted media items.

### Matching Component

Calculates deterministic match scores between the user's profile and candidate media.

### Candidate Component

Provides candidate media items that can be considered for matching and decision mode.

### Decision Component

Applies explicit filtering rules to help the user narrow choices according to mood, time, commitment, status, and other planned criteria.

### Dashboard Component

Provides aggregate summaries of the user's collection, profile, candidates, and matching state.
