# API Contract

This document describes the REST API shape for MoodMatch. Some endpoints are already implemented, while others remain planned.

The API will use REST endpoints with JSON request and response bodies. Backend responses will use DTOs instead of exposing persistence entities directly.

## Media

| Method | Endpoint | Planned purpose |
| --- | --- | --- |
| GET | `/api/media` | List media items. |
| GET | `/api/media/{id}` | Get one media item by id. |
| POST | `/api/media` | Create a media item. |
| PUT | `/api/media/{id}` | Replace a full media item resource. |
| DELETE | `/api/media/{id}` | Delete a media item. |
| PUT | `/api/media/{id}/tags` | Replace the full tag list for a media item. |
| PATCH | `/api/media/{id}/status` | Change only the consumption status field. |
| PATCH | `/api/media/{id}/favorite` | Change only the favorite field. |

## Tags

| Method | Endpoint | Planned purpose |
| --- | --- | --- |
| GET | `/api/tags` | List available tags. |

## Profile

| Method | Endpoint | Planned purpose |
| --- | --- | --- |
| GET | `/api/profile` | Return the calculated user interest profile. |

## Candidates

| Method | Endpoint | Planned purpose |
| --- | --- | --- |
| GET | `/api/candidates` | Return candidate media items for matching or decision mode. |

## Matches

| Method | Endpoint | Planned purpose |
| --- | --- | --- |
| GET | `/api/matches` | Return deterministic match results with explainable scoring details. |

## Dashboard

| Method | Endpoint | Planned purpose |
| --- | --- | --- |
| GET | `/api/dashboard` | Return dashboard summary data. |

## External Search

Phase 18 adds a preview-only external search endpoint. It uses a deterministic offline demo provider and does not import or update local media yet.

| Method | Endpoint | Purpose |
| --- | --- | --- |
| GET | `/api/external/search` | Search normalized external preview results from the demo provider. |

### Query Parameters

| Name | Required | Type | Notes |
| --- | --- | --- | --- |
| `query` | yes | string | Search term, trimmed server-side. |
| `mediaType` | yes | enum | One of `FILM`, `SERIES`, `BOOK`, `GAME`. |
| `source` | no | enum | Phase 18 supports `DEMO` only. Defaults to `DEMO`. |
| `limit` | no | integer | Optional positive limit. Backend applies a safe default and maximum. |

### Response Shape

```json
{
  "query": "arrival",
  "mediaType": "FILM",
  "source": "DEMO",
  "results": [
    {
      "source": "DEMO",
      "externalId": "demo-film-arrival",
      "mediaType": "FILM",
      "title": "Arrival",
      "originalTitle": null,
      "description": "A normalized preview description.",
      "releaseYear": 2016,
      "coverUrl": "https://demo.moodmatch.local/covers/arrival.jpg",
      "sourceUrl": "https://demo.moodmatch.local/items/demo-film-arrival",
      "externalGenres": ["Science-Fiction", "Drama"],
      "externalSubjects": ["Zeit", "Entdeckung"],
      "suggestedTags": [],
      "attribution": "MoodMatch Demo Provider (offline)",
      "warnings": []
    }
  ],
  "warnings": []
}
```

Notes:

- `source=DEMO` is the only supported source in Phase 18.
- Results are normalized and deterministic so the provider architecture can be tested offline.
- This endpoint is preview-only. No external search result can be imported into the local media library yet.

## Update Semantics

`PUT` replaces full resources or full collections addressed by the endpoint. For example, `PUT /api/media/{id}` replaces the media item resource, and `PUT /api/media/{id}/tags` replaces the full tag list for the media item.

`PATCH` changes only the addressed field or partial state. For example, `PATCH /api/media/{id}/status` changes only the status field, and `PATCH /api/media/{id}/favorite` changes only the favorite field.
