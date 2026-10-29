# LaSync v1.0.3 Hotfix Release Notes 🚀⚡

## 🚨 Critical Fix: Android 14/15 MediaProjection Crash Resolved

### What Happened
In `v1.0.2`, tapping **SHARE SYSTEM MUSIC** on Android 14 and Android 15 (e.g., iQOO Neo 10 / API 34 & 35) threw a fatal `SecurityException`:
```
java.lang.SecurityException: Media projections require a foreground service of type ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
    at android.media.projection.MediaProjectionManager.getMediaProjection(...)
    at com.lasync.audio.MainActivity.mediaProjectionLauncher(...)
```

### Root Cause
Calling `context.startForegroundService()` from `MainActivity` is asynchronous. In Android 14+, the OS strictly enforces that `startForeground(..., FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)` must be actively running before `getMediaProjection(resultCode, data)` can be called. Because `MainActivity` invoked `getMediaProjection` immediately on the main thread before the service's `onStartCommand` executed, the system blocked the call with a `SecurityException`.

### The Hotfix in v1.0.3
1. **Synchronous In-Service Delegation**:
   - `MainActivity` now delegates the `resultCode` and `resultData` Intent directly to `LaSyncForegroundService.startWithMediaProjection(...)`.
   - Inside `LaSyncForegroundService.onStartCommand()`, `startForeground(..., FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)` is called *first*, synchronously updating the service state in the OS.
   - Immediately following `startForeground`, `projectionManager.getMediaProjection(...)` is obtained safely without permission rejections.
2. **Android 14/15 Required Callback Registration**:
   - Registered `MediaProjection.Callback` immediately upon projection creation, satisfying Android 14/15 requirements and preventing `IllegalStateException`.
3. **Clean Teardown & Projection Release**:
   - Tapping **STOP MUSIC SHARING** now cleanly calls `MediaProjection.stop()` and transitions the foreground service back to `FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE`.
