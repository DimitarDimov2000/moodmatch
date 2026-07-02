# Deprecated Google/OIDC Setup Reference

This guide is no longer active setup documentation.

MoodMatch originally explored Google OAuth/OIDC, but that path was removed from the normal prototype flow because it depended on external Google Cloud project/client state. The OAuth client/project was disabled, which made the sign-in path too fragile for the university prototype.

The active prototype auth path is local email/password:

```env
VITE_AUTH_MODE=local-password
VITE_AUTH_PROVIDER=local-password
```

Do not configure Google Identity Services for the current app. Do not add `VITE_GOOGLE_CLIENT_ID` as required setup. Do not commit Google client secrets.

This file remains only as a historical reference that Google OAuth was considered and intentionally abandoned for the prototype.
