# LaSync v1.0.6 Release Notes 🎵📡

## 🌟 What's New in v1.0.6: "TruePhase" (Dual Buffer Modes & Hardware Timestamps)

Achieve laboratory-grade acoustic synchronization and complete immunity to Wi-Fi drops! `v1.0.6` delivers our milestone acoustic architecture:

---

### 1. Dual Buffer Profiles: Ultra-Stable Music vs. Low-Latency Video
The Host can now dynamically switch between two custom acoustic synchronization profiles:
* **🎵 Ultra-Stable Music Mode (`1.0s` deep RAM buffer)**:
  - Allocates a massive 100-frame circular RAM cushion on all receiver devices.
  - Completely immune to Wi-Fi packet drops, room movements, and router congestion. Your music will never stutter or drop.
* **🎬 Low-Latency Video Mode (`150ms` buffer)**:
  - Tight 15-frame buffer optimized for lip-sync accuracy when watching YouTube videos, movies, TikTok, or gaming.
* **Instant Dynamic Adaptation**: Receivers automatically read the mode tag from incoming UDP packets and adjust their playback thresholds in real time without dropping the connection.

---

### 2. Hardware Presentation Timestamps (`AudioTrack.getTimestamp()`)
* **Physical DAC & Speaker Coil Tracking**: Instead of guessing when audio leaves the Android buffer, `ReceiverAudioEngine` queries the Linux kernel and audio HAL directly via `AudioTrack.getTimestamp(AudioTimestamp)`.
* **Microsecond Phase-Lock Error Calculation**: Measures the exact nanosecond delay between the speaker driver and the Master Host timeline.
* **Real-Time Phase Metrics Display**:
  - Live DAC Latency (e.g. `28.5 ms`)
  - Sub-millisecond Phase Alignment (e.g. `±0.8 ms`)
  - Dynamic `PHASE-LOCKED` verification badge on the Receiver dashboard.

---

### 3. Cyber-Acoustic TruePhase UI
* **Host Screen Profile Switcher**: Interactive selector allowing the host to switch between Music and Video profiles with a single tap.
* **Receiver Screen Hardware Sync Card**: Dedicated status module displaying DAC latency, phase offset, and profile telemetry.

