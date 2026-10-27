# LocalSync Audio 🎵📡

LocalSync Audio is an offline, synchronized, one-to-many audio distribution system for Android. A single **Host** phone plays audio and streams timestamped Opus frames over local Wi-Fi to **N Receiver** phones. Each receiver independently routes audio to its own Bluetooth headphones and synchronizes playback against the host's master timeline using sub-millisecond clock synchronization, a jitter buffer, micro-drift correction, and per-device Bluetooth acoustic offset calibration.

---

## 🏛 Architecture

```text
                         HOST PHONE
                      🎵 Master Clock
                            │
                     UDP Control (8888)
                     UDP Audio   (8889)
                            │
                    LOCAL WI-FI NETWORK
                            │
             ┌──────────────┼──────────────┐
             ↓              ↓              ↓
          PHONE B         PHONE C        PHONE D
             │              │              │
          Bluetooth      Bluetooth      Bluetooth
             │              │              │
            🎧 B            🎧 C            🎧 D
```

---

## 🚀 Status: Phase 1 (Completed)

- **Transport**: Real-time UDP socket engine on port `8888`.
- **Discovery**: Automatic Host beacon broadcasting (`255.255.255.255` & subnet) and client discovery.
- **Clock Synchronization**: Sub-millisecond NTP-lite 4-timestamp engine using `SystemClock.elapsedRealtimeNanos()`.
- **Metrics**: Real-time RTT calculation, RFC 3550 Jitter estimation, packet loss tracking, and sliding-window median spike rejection.
- **Multi-Receiver Scalability**: Host tracks N receivers concurrently with automatic heartbeat pruning.
- **Background Execution**: Android Foreground Service with `WIFI_MODE_FULL_LOW_LATENCY` and `PARTIAL_WAKE_LOCK`.
- **UI**: Jetpack Compose OLED dark aesthetic with animated radar pulse indicators and live telemetry cards.

---

## 🛠 Building & Running

### Requirements
- Android SDK (API 35 compileSdk, minSdk 26)
- JDK 21 LTS
- Physical Android devices or emulators on the same Wi-Fi router / LAN

### Build APK
```bash
./gradlew assembleDebug
```
The debug APK is generated at:
`app/build/outputs/apk/debug/app-debug.apk`

### Run Unit Tests
```bash
./gradlew test
```

### Install via ADB
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 📱 How to Use (Phase 1)

1. Connect both phones to the same Wi-Fi network.
2. **Phone 1 (Host)**: Open app → Tap **[ CREATE SESSION ]**.
3. **Phone 2 (Receiver)**: Open app → Tap **[ JOIN SESSION ]** → Tap **[ JOIN ]** on the discovered host.
4. Watch the live synchronization telemetry (RTT, Jitter, Clock Offset, Packet Loss) update in real time!
