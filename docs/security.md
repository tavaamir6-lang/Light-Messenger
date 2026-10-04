# Security baseline

1. Never commit production JWT secrets, database credentials or release keys.
2. Production uses HTTPS/WSS only.
3. Apply rate limits to authentication endpoints.
4. Validate all request bodies before persistence.
5. Do not log passwords, tokens or message bodies in production.
6. Encrypt sensitive local storage on Android.
7. Do not advertise end-to-end encryption until the cryptographic design is reviewed.
