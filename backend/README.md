# MoodMatch Backend

This directory contains the Quarkus Maven backend scaffold for MoodMatch.

The backend currently includes only the foundation needed to verify that the application starts. Business logic, database access, domain entities, DTOs, media management, tags, profile calculation, and matching logic are intentionally not implemented yet.

## Running In Dev Mode

```powershell
.\mvnw.cmd quarkus:dev
```

The application starts on the default Quarkus port:

```text
http://localhost:8080
```

Health check:

```text
GET /api/health
```

Expected response:

```json
{
  "status": "UP"
}
```

## Running Tests

```powershell
.\mvnw.cmd test
```
