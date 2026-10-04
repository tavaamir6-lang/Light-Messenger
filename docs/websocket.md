# WebSocket protocol

Endpoint: `wss://<host>/ws`

Server → client:
```json
{"type":"ready","service":"light-messenger","version":"0.1.0"}
```

Client → server:
```json
{
  "type": "message.send",
  "requestId": "uuid",
  "conversationId": "uuid",
  "clientMessageId": "uuid",
  "body": {"text": "سلام"}
}
```

Every outbound message gets a client-generated id so retries can be deduplicated server-side.

Receipt states: queued, sent, delivered, read, failed.
