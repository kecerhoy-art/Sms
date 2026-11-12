# SMS Sync — starter project (website + Android APK source)

This is a **consent-based starter**, not a finished hosted service or signed APK.
It forwards **new incoming SMS only** from an Android phone after the phone owner
grants Android's SMS permission. It does not hide the app, read messages while the
phone is off, or bypass Android permission controls.

## Contents
- `server/`: Node.js + Express + SQLite API and a small web dashboard.
- `android/`: Android Studio Kotlin project source for a sideloadable APK.

## 1. Run the server locally
Requirements: Node.js 20+.

```bash
cd server
cp .env.example .env
# Edit .env and set strong ADMIN_PASSWORD and DEVICE_TOKEN values.
npm install
npm start
```

Open `http://localhost:3000` on the same network for a local demo.
For use across the internet, deploy behind HTTPS (for example, a VPS with a
reverse proxy and TLS). Do not expose plain HTTP to the public internet.
The server binds to `0.0.0.0`; configure firewall access carefully.

Login with the `ADMIN_PASSWORD` from `.env`. The dashboard uses a session token
stored in the browser's session storage.

## 2. Build the Android APK
Requirements: Android Studio, Android SDK, and Gradle supplied by Android Studio.

1. Open the `android/` folder in Android Studio.
2. In `MainActivity.kt`, set `API_BASE_URL` to your server's HTTPS address.
3. Build > Build Bundle(s) / APK(s) > Build APK(s).
4. Install the generated APK on HP 2. Android may ask you to allow installation
   from that source.
5. Open the app, enter the device token configured in the server `.env`, and
   explicitly grant SMS permission.

The app displays its purpose and permission request. Do not remove that disclosure.

## 3. Pairing and testing
- Keep the device token private. Anyone with it can submit SMS to this server.
- Send a test SMS to HP 2. With permission granted and internet available, the
  new incoming message should appear on the website.
- The app queues messages for upload when connectivity returns.
- The server dashboard is protected by the admin password.

## Important limitations
- This starter forwards only **new incoming messages** after setup; it does not
  import the phone's existing SMS history.
- HP 2 must be powered on. If it is off, there is no live monitoring.
- Android battery optimization and manufacturer background limits may delay uploads.
- SMS content is sensitive. Use HTTPS, strong secrets, restrict server access,
  and delete stored data when no longer needed.
- SMS permissions are restricted by Google Play policy. Sideloading avoids Play
  Store distribution, but does not remove Android permission requirements.
- This is a development starter. Before real use, add account recovery, rate limits,
  audit logging, retention controls, backups, and production security review.
