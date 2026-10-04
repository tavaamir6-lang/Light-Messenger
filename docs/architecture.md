# Architecture

## Client
Presentation → ViewModel/State → Domain → Repository → Remote/Local data.

Modules are separated by feature so auth, chat, groups, media and settings can evolve independently.

## Transport
- HTTPS REST for account, configuration and history.
- WSS over TLS for real-time events.
- Exponential backoff and reconnect policy in the transport layer.
- Outbound queue preserves messages during temporary offline periods.

## Server
Fastify handles HTTP APIs. WebSocket handles live events. PostgreSQL is the durable source of truth. Redis is for presence, short-lived state and fan-out.

## Availability
The client will support a prioritized endpoint list. The goal is resilience and failover in constrained networks, not a guarantee of bypassing every network restriction.

## Security
Passwords are hashed. Access tokens are rotatable. TLS is mandatory in production. Secrets remain outside source control. E2E encryption will only be claimed after a reviewed cryptographic protocol and threat model are implemented.
