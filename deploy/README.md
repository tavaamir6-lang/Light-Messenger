# Light Messenger deployment

## Required server variables

Set these on the server, never inside the Android APK:

- POSTGRES_PASSWORD: strong random PostgreSQL password
- JWT_SECRET: long random secret
- LIGHT_MESSENGER_ACCESS_CODE: 1389
- CORS_ORIGIN: the exact allowed API origin(s)

## Start the stack

From deploy/docker:

```bash
export POSTGRES_PASSWORD='change-me'
export JWT_SECRET='generate-a-long-random-secret'
export LIGHT_MESSENGER_ACCESS_CODE='1389'
export CORS_ORIGIN='*'
docker compose up -d --build
```

PostgreSQL is initialized automatically from database/migrations/001_initial.sql when the database volume is created for the first time.

The API listens on port 8080. Put it behind an HTTPS reverse proxy in production and expose only HTTPS/WSS publicly.

## Android connection

The Android app must be built with its real HTTPS API base URL instead of the placeholder value. The WebSocket endpoint is derived automatically as WSS for HTTPS.

Do not put JWT secrets, PostgreSQL passwords, or the access-code configuration in the APK or repository.

A GitHub repository cannot create a public VPS/domain by itself. A real server host and HTTPS domain are still required for an internet-accessible deployment.
