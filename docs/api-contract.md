# API Contract

This document describes the planned REST API for MoodMatch. These endpoints are not implemented yet.

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

## Update Semantics

`PUT` replaces full resources or full collections addressed by the endpoint. For example, `PUT /api/media/{id}` replaces the media item resource, and `PUT /api/media/{id}/tags` replaces the full tag list for the media item.

`PATCH` changes only the addressed field or partial state. For example, `PATCH /api/media/{id}/status` changes only the status field, and `PATCH /api/media/{id}/favorite` changes only the favorite field.
