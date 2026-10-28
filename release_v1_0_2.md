# LaSync v1.0.2 Release Notes 🎵📡

## 🌟 What's New in v1.0.2

### 1. Fix: Eradicated Receiver "Rain Noise" (Static)
During Phase 2 real-device testing with the 440 Hz test tone, the receiver produced a loud "rain noise" (white noise/static).
**Root Causes Identified & Solved:**
* **Silence Overfill**: On receiver startup, pre-filling 46,080 bytes of silence into a 48,000-byte hardware buffer pushed the `AudioTrack` buffer to 96% capacity.
* **16-bit PCM Odd-Byte Shredding (`WRITE_NON_BLOCKING`)**: Because the hardware buffer was nearly full, subsequent 960-byte PCM frame writes were partially truncated on odd-byte boundaries. In 16-bit signed PCM, truncating an odd number of bytes swaps the low byte with the high byte (inverting magnitude and sign bits). This instantly corrupts a clean 440 Hz sine wave into loud pseudo-white noise ("rain noise").
* **Wi-Fi Jitter Underruns**: Live jitter measured 36.0 ms on the test phones. Direct non-buffered writes triggered sporadic hardware underruns.

**The Fix in v1.0.2:**
* Replaced the direct write pipeline with a **Dedicated Concurrent Memory Jitter Buffer** (`ConcurrentLinkedQueue<ByteArray>`) with 200 ms lead buffering.
* Configured native **Stereo AudioTrack** (`AudioFormat.CHANNEL_OUT_STEREO`) and guaranteed full-frame writes using `AudioTrack.WRITE_BLOCKING`. Odd-byte misalignment is now mathematically impossible.

---

### 2. New Feature: System Music Sharing (Spotify, YouTube, Games)
You can now share ANY audio playing on the Host phone to all connected listener phones in real-time!
* **Google `AudioPlaybackCapture` Engine**: Uses Android 10+ (API 29+) audio playback capture architecture matching `USAGE_MEDIA`, `USAGE_GAME`, and `USAGE_UNKNOWN`.
* **Privacy Compliance**: Phone calls (`USAGE_VOICE_COMMUNICATION`) are strictly excluded per Android security standards.
* **Dual Audio Output**: The Host continues playing music out loud through its own phone speakers, while simultaneously streaming raw 48 kHz PCM frames over UDP Port `8889` to all receivers.
* **One-Tap Permission Flow**: Simply tap **[ SHARE SYSTEM MUSIC ]** on the Host screen, accept the standard Android Media Projection prompt, and start playing music on Spotify, YouTube, Apple Music, or any media app.
