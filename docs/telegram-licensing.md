# Telegram code and licensing

The official Telegram Android repository is publicly available under GPL-2.0:
https://github.com/TelegramOrg/Telegram-Android

Rules for Light Messenger:
- Do not copy Telegram branding or its standard logo.
- Do not claim this application is official Telegram.
- Any Telegram-derived source retained here must keep applicable copyright and GPL notices.
- Project-owned source is intended for GPL-2.0-or-later.
- Third-party libraries retain their own licenses.

Technical distinction:
Telegram Android is a client for Telegram infrastructure and MTProto. Copying its Android client does not create an independent messenger server. Light Messenger therefore starts with its own REST/WebSocket protocol and can selectively incorporate compatible open-source components later.


Implementation plan:
- Use Telegram as a UX/architecture reference for chat lists, message states, media pipelines, reconnect behavior and notification flows.
- Keep Light Messenger identity, backend, API and storage independent.
- If Telegram GPL source is incorporated later, isolate it and preserve the required license notices; do not mix proprietary code into a GPL-derived component without checking license compatibility.
