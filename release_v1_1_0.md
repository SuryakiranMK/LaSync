# 🎵 LaSync v1.1.0 — RAMJukebox (Offline Zero-Traffic Audio Player)

Welcome to **LaSync v1.1.0** — introducing **RAMJukebox**, our zero-traffic offline playback engine that completely eliminates Wi-Fi drops, jitter, and network congestion by streaming and playing uncompressed audio directly from device memory.

---

### 🚀 What's New in v1.1.0

#### 1. 📂 Local Storage File Picker (Storage Access Framework)
- The Master Host can now pick any local audio file (**MP3, FLAC, WAV, AAC, M4A**) directly from internal storage or SD card via Android's native Storage Access Framework (`audio/*`).

#### 2. ⚡ Hardware-Accelerated PCM Decoding
- Utilizes Android's hardware `MediaExtractor` and `MediaCodec` pipelines to decode compressed audio into raw 16-bit 48/44.1 kHz stereo PCM byte buffers.
- Includes a live decoding progress bar on the Host screen (`0% to 100%`).
- Automatically balances mono tracks to uniform stereo frame alignment (`AudioConstants.monoToStereo`).

#### 3. 🚀 High-Speed TCP Blast Server & Client (Port 8890)
- Master Host spins up an ultra-fast TCP socket server on port `8890`.
- Once decoded, the raw PCM track is streamed into all connected listeners' device RAM in **1–2 seconds** over high-throughput Wi-Fi LAN sockets.
- Receivers display a live download progress bar and report readiness via the `TYPE_JUKEBOX_READY` handshake.

#### 4. 🔕 Scheduled Microsecond Trigger & ZERO Wi-Fi Traffic Playback
- Master Host schedules a synchronized trigger timestamp ($T_{\text{start}} = \text{nowMasterTime} + 400\text{ms}$).
- All phones start playback simultaneously directly from internal RAM buffers.
- **ZERO Wi-Fi network traffic during playback**: Wi-Fi throughput drops to 0 KB/s during playback, making the audio stream 100% immune to router lag, packet drops, or microwave interference.

#### 5. 🎛️ Synchronized Playback Controls & Seeking
- **Interactive Seek Bar**: Scrub to any point in the song; the Host computes the microsecond frame offset and synchronizes all listener devices in phase.
- **Synchronized Play/Pause/Stop**: Seamless state synchronization across Host and listener UI consoles.
- **Live Cyber-Acoustic Timeline**: Displays elapsed vs total time (`mm:ss / mm:ss`) and active RAM playback indicators.

---

### 📦 Installation
Download and install `LaSync.apk` directly on all Android devices (Android 8.0+ / API 26+).
