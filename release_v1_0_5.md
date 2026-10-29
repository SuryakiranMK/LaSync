# LaSync v1.0.5 Release Notes 🎵📡

## 🌟 What's New in v1.0.5: "CyberShade" (Live Notification & Background QoL)

Never lose track of your audio sync session again! `v1.0.5` introduces a complete notification-based control surface and bulletproof background resilience:

### 1. Spotify-Style Live Media Notification
* **Live Dynamic Telemetry**:
  - *Host Mode*: Displays active session ID, linked listener count, and live frame streaming telemetry (`"🎵 Streaming System Music to 2 phone(s) • 420 frames"`).
  - *Receiver Mode*: Displays connection link, host name, RTT, jitter, and packet loss in real-time (`"🟢 Linked to Host • RTT 18ms • Loss 0.0%"`).
* **Lock-Screen Visibility**: Configured with `VISIBILITY_PUBLIC` and `FOREGROUND_SERVICE_IMMEDIATE` so the live synchronization card is fully accessible without unlocking your phone.

---

### 2. Lock-Screen Action Buttons
Control your audio session straight from Android's notification shade or lock screen:
* **`[ ⏹ Stop Session ]`**: Tears down the network session, releases locks, and gracefully returns the app to the home screen.
* **`[ 🔇 Stop Audio ]`**: Appears whenever music or acoustic tone test streaming is active, allowing you to instantly pause/mute the broadcast without closing the session.

---

### 3. Bulletproof Background Persistence
* Service elevated to `START_STICKY` with continuous `WIFI_MODE_FULL_LOW_LATENCY` and `PARTIAL_WAKE_LOCK`.
* Immune to Android aggressive battery-savers or Doze mode when switching to Spotify, YouTube, games, or locking the device screen.
