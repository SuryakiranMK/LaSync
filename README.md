# LaSync 🎵📡

<p align="center">
  <img src="logo.jpg" alt="LaSync Logo" width="160" style="border-radius: 36px; box-shadow: 0 8px 32px rgba(0, 242, 254, 0.3);" />
</p>

<p align="center">
  <b>Local Acoustic Synchronizer</b><br/>
  <i>Phase-Locked Multi-Device Audio Timeline Distribution</i>
</p>

<p align="center">
  <a href="https://github.com/SuryakiranMK/LaSync/releases/download/v1.0.8/LaSync.apk">
    <img src="https://img.shields.io/badge/Download%20APK-v1.0.8-00F2FE?style=for-the-badge&logo=android&logoColor=black" alt="Download APK" />
  </a>
  <a href="https://github.com/SuryakiranMK/LaSync/releases/latest">
    <img src="https://img.shields.io/badge/Release-v1.0.8-9D4EDD?style=for-the-badge" alt="Release Version" />
  </a>
</p>

---

## 🏛 Technical Expansion
**LaSync** stands for:
* **L**ocal **A**udio **Sync**hronization Engine
* **L**ow-latency **A**coustic **Sync**hronizer
* **L.A.S.Y.N.C.** $\rightarrow$ **L**ocal **A**coustic **SYN**chronous **C**ast

---

## 🏛 System Architecture

```text
                         HOST PHONE
                    🎵 Master Clock Node
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

## 🎨 Visual Identity & Custom Design System

LaSync features a proprietary **"Cyber-Acoustic Precision"** design language:
- **Palette**: Void Obsidian (`#060811`), Frosted Carbon Glass (`#0D1322`), HyperCyan (`#00F2FE`), Ultraviolet (`#9D4EDD`), and Quantum Mint (`#00F5A0`).
- **Harmonic Wave Equalizer**: A 5-bar live acoustic visualizer that breathes with audio timeline events.
- **Concentric Acoustic Radar**: Multi-ring ripple animations that reflect discovery and link health.
- **Phase-Lock Latency Dial**: Hero gauge showing sub-millisecond hardware clock offsets in real time.

---

## 🚀 Features & Capabilities
 
### Phase 1: High-Precision LAN Time Sync
- **Transport**: Real-time non-blocking UDP socket engine on port `8888`.
- **Discovery**: Automatic Host beacon broadcasting (`255.255.255.255` & subnet) and client discovery.
- **Clock Synchronization**: Sub-millisecond NTP-lite 4-timestamp engine using monotonic `SystemClock.elapsedRealtimeNanos()`.
- **Metrics**: Real-time RTT calculation, RFC 3550 Jitter estimation, packet loss tracking, and sliding-window median spike rejection.
- **Multi-Receiver Scalability**: Host tracks N receivers concurrently with automatic heartbeat pruning.
- **Background Execution**: Android Foreground Service with `WIFI_MODE_FULL_LOW_LATENCY` and `PARTIAL_WAKE_LOCK`.

### Phase 2: Synchronized Audio Streaming & Music Sharing
- **System Music Sharing (Spotify, YouTube, Games)**: Share all live device audio from your phone to connected listeners in real time using Google's `AudioPlaybackCapture` API (excluding phone calls for privacy).
- **Acoustic Benchmark Mode**: 440 Hz concert pitch 'A' sine wave generator for verifying microsecond-accurate acoustic phase alignment.
- **TruePhase Dual Buffer Modes**: Ultra-Stable Music Mode (1.0s deep RAM cushion) for zero dropouts, and Low-Latency Video Mode (150ms buffer) for lip-sync accuracy.
- **Hardware Presentation Timestamps**: Direct `AudioTrack.getTimestamp()` tracking of physical DAC latency and phase alignment.
- **Multi-Client UDP Audio Multicast**: Broadcasts raw 48 kHz 16-bit PCM frames on Port `8889` across the entire local Wi-Fi subnet.

### Phase 3: GhostMegaphone (Stealth Walkie-Talkie & PA System)
- **Stealth Push-To-Talk (PTT)**: Long-press / hold the glowing logo on the Host or the dedicated Cyber-Crimson PA button to instantly broadcast your live voice.
- **Hardware AEC Voice Capture**: 48 kHz mono microphone stream with acoustic echo cancellation and noise suppression.
- **Instant Music Ducking & Un-Ducking**: Automatically ducks/mutes background music streams during speech and smoothly restores music when released.
- **Remote 100% Volume Boost**: Receivers automatically override `STREAM_MUSIC` to maximum volume and play voice with zero jitter delay (<20ms).
- **Clean Volume Restoration**: Restores each receiver's volume back to their original level once the broadcast ends.

### Phase 4: SoundBoard (Host Multi-Device Mixer Console)
- **Per-Device Delay Trim (`+/- ms`)**: Tune out room echoes and Bluetooth headphone codec delays per phone (-250ms to +350ms) with microsecond precision.
- **Hardware Route Detection**: Automatically detects and displays whether each receiver outputs to 🎧 Bluetooth, 🔊 Built-in Speaker, or 🔌 USB DAC.
- **Remote Mixer Strip Controls**: Per-phone remote volume steppers (`-10%`, `+10%`), channel MUTE, studio SOLO isolation, and remote DROP/disconnect.

---

## 🛠 Building & Running

### Requirements
- Android SDK (API 35 compileSdk, minSdk 26)
- JDK 21 LTS

### Build APK
```bash
./gradlew assembleDebug
```
Output:
`app/build/outputs/apk/debug/app-debug.apk`

### Run Unit Tests
```bash
./gradlew test
```

### Install via ADB
```bash
adb install -r LaSync.apk
```

---

## 📱 How to Use

1. Connect all phones to the same Wi-Fi network.
2. **Phone 1 (Host)**: Open LaSync → Tap **[ CREATE SESSION ]**.
3. **Phone 2+ (Receivers)**: Open LaSync → Tap **[ JOIN SESSION ]** → Tap **[ JOIN ]** on the discovered host.
4. **To Share Music**: On the Host, tap **[ SHARE SYSTEM MUSIC ]**, approve the permission prompt, open Spotify or YouTube, and play any song!
5. **To Test Tone Sync**: Tap **[ START 440 Hz ACOUSTIC TEST ]** to hear pure, synchronized concert pitch 'A' playing across all devices simultaneously!
6. **To Broadcast via GhostMegaphone**: On the Host, press and hold the glowing LaSync logo at the top or the **[ HOLD TO TALK ]** button to speak. All connected phones will instantly boost to 100% volume and broadcast your voice with zero delay! Release to un-duck music and restore previous receiver volumes.
7. **To Fine-Tune Multi-Phone Sync (SoundBoard)**: On the Host screen, each connected phone appears as a mixing strip. Tap `[ -10ms ]` / `[ +10ms ]` to compensate for Bluetooth delays or room echoes, adjust volume remotely, or tap **[ SOLO ]** to isolate one phone!
