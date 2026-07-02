# MoodMatch Auth Provider Decision

## 1. Purpose

MoodMatch is moving toward a deployable university prototype for several private daily users.

The project has already introduced the user ownership foundation:

- `app_users`
- `media_items.user_id`
- user-scoped media/profile/candidate/matching logic
- temporary local demo user resolution

This document decides the authentication provider direction and deployment authentication shape before implementation.

## 2. Deployment Shape

Chosen deployment shape:

```txt
Separate frontend and backend deployment.