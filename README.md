# Light Messenger

پیام‌رسان اندرویدی با معماری مستقل کلاینت/سرور، رابط فارسی و RTL و ارتباط لحظه‌ای HTTPS/WebSocket.

## هدف نسخهٔ اول
- ورود و ثبت‌نام
- پروفایل و کاربران
- چت خصوصی
- WebSocket real-time
- صف آفلاین و retry
- گروه‌ها و رسانه در مراحل بعد
- چند endpoint برای افزایش availability در شبکه‌های محدود
- GitHub Actions برای ساخت APK

## ساختار
- `android/` کلاینت Android با Kotlin + Jetpack Compose
- `server/` API و WebSocket با Node.js + TypeScript
- `database/` migration و schema
- `deploy/` Docker و reverse proxy
- `docs/` معماری، API، WebSocket و امنیت
- `.github/workflows/` CI/CD

## تکنولوژی
Android: Kotlin 2.4.20, Jetpack Compose, Material 3, Ktor Client, Coroutines/Flow, minSdk 26, targetSdk 37.

Server: Node.js, TypeScript, Fastify, WebSocket, PostgreSQL, Redis.

## راه‌اندازی
### Server
```bash
cd server
npm install
npm run dev
```

### Android
ریشه پروژه را با Android Studio باز کنید یا workflow گیت‌هاب را اجرا کنید.

## متغیرهای محیطی
```
PORT=8080
DATABASE_URL=postgres://light:light@postgres:5432/lightmessenger
REDIS_URL=redis://redis:6379
JWT_SECRET=replace-in-production
CORS_ORIGIN=*
```

## Telegram source policy
منبع رسمی Telegram Android تحت GPL-2.0 است. در صورت استفاده از اجزای GPL باید notices و شرایط همان مجوز حفظ شوند. این پروژه از نام و لوگوی Telegram استفاده نمی‌کند و فعلاً پروتکل مستقل Light Messenger را دارد.

منبع رسمی: https://github.com/TelegramOrg/Telegram-Android

## وضعیت
Scaffold / foundation — در حال توسعه.
