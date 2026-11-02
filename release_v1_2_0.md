# ⚡ LaSync v1.2.0: ZeroLagSync — Ultra-Low Latency & TruePhase Sync

LaSync **v1.2.0** is a major synchronization and audio performance upgrade, eliminating high latency, driver buffer bloat, and room echo across connected devices.

---

### 🚀 Key Improvements in v1.2.0

* **🏎️ Low-Latency Hardware Buffer Optimization**:
  * Slashed Receiver and Host `AudioTrack` buffer headroom from 150 frames (1,500 ms) down to 6–12 frames (60–120 ms) in `PERFORMANCE_MODE_LOW_LATENCY`.
  * Instantly strips out ~1.4 seconds of Android AudioFlinger / HAL driver latency.

* **⏱️ Clock-Locked Presentation Scheduling**:
  * Receiver dynamically synchronizes audio writes against NTP master monotonic timestamps (`clockSyncEngine.toLocalTimeNanos`), releasing audio to the DAC at the exact scheduled sub-millisecond instant.
  * Completely stops unpaced buffer dumping into the hardware queue.

* **🛡️ Dynamic Drift Compensation & Late Frame Dropping**:
  * Automatically discards frames arriving >40ms late due to Wi-Fi hiccups or packet retransmissions, preventing latency accumulation and keeping devices locked together over extended listening sessions.

* **⚡ Three Dynamic Sync Profiles**:
  * **Ultra-Low Party (50ms lead time)**: Default profile for multi-speaker room sync with zero audible echo.
  * **Low-Latency Video (100ms lead time)**: Optimized for lip-sync accuracy when streaming movies or YouTube.
  * **Ultra-Stable Music (300ms lead time)**: Deep RAM buffer for congested or lossy Wi-Fi environments.

* **🎛️ SoundBoard Delay Trim Functional Lock**:
  * Coupled the Host SoundBoard `delayTrimMs` slider (-250ms to +350ms) directly to Receiver presentation pacing, allowing users to fine-tune acoustic phase alignment in real time.

* **⚡ Fast System Capture & Jukebox Trigger**:
  * Reduced `AudioRecord` internal capture buffer to ~40ms and tightened Jukebox scheduled trigger lead time to 150ms for instantaneous playback start.

---

### 📦 Installation
Download and install `LaSync.apk` directly on all Android devices (Android 8.0+ / API 26+).
