# 🛠️ LaSync v1.1.1 Hotfix — Active Connection Session Persistence

**LaSync v1.1.1 Hotfix** resolves the session reset issue where pressing Home or Back while connected or hosting navigated the user to the landing screen upon reopening the app.

---

### 🐛 Bug Fixes & Improvements

1. **Back Gesture Minimization (`BackHandler`)**:
   - Pressing the system Back button or performing a back gesture inside `HostScreen` or `ReceiverScreen` now delegates to `moveTaskToBack(true)`.
   - The app safely minimizes to the background while the active synchronization session, audio streams, and foreground service remain alive.

2. **Single-Task Activity Launch Mode (`launchMode="singleTask"`)**:
   - Configured `android:launchMode="singleTask"` on `MainActivity` so reopening the app from the launcher or recents directly resumes the active task without resetting UI state.

3. **Application-Scope Engine Lifecycle (`LaSyncApp`)**:
   - Migrated `HostNetworkEngine` and `ReceiverNetworkEngine` to persistent application-level scope (`LaSyncApp.applicationScope`), decoupling active network and audio threads from volatile Activity destruction.

4. **Real-Time Screen State Reconciliation**:
   - `MainActivity` checks live engine states (`hostEngine.isRunning` and `receiverEngine.state != IDLE`) on `onCreate()`, `onResume()`, and `onNewIntent()`, guaranteeing the user is always presented with their active session.

5. **Safe Session Teardown**:
   - Removed destructive engine termination from `onDestroy()`; sessions now terminate only when the user explicitly taps "Close" / "Stop Session" in the UI or Notification Shade.

---

### 📦 Installation
Download and install `LaSync.apk` directly on your Android device (Android 8.0+ / API 26+).

