# 🛠️ LaSync v1.1.3 Hotfix — Zero Audio Persistence on Stop

**LaSync v1.1.3 Hotfix** completely resolves the issue where music or audio playback persisted after the host stopped streaming or closed the session.

---

### 🚀 What's New & Fixed in v1.1.3

1. **Synchronized `TYPE_AUDIO_STOP` Signal**:
   - Master Host immediately broadcasts an explicit `TYPE_AUDIO_STOP` control packet across all receivers when music sharing, tone test, or session stops.

2. **Instant Receiver Buffer Flush**:
   - Receivers clear the jitter queue instantly, pause and flush hardware `AudioTrack` DAC buffers immediately upon receiving `TYPE_AUDIO_STOP` or `TYPE_REMOTE_DISCONNECT`.
   - Reduced stream idle timeout from 1200ms to 300ms to cut any lingering silence frames.

3. **Clean Host Audio Record & Projection Teardown**:
   - Immediate cancellation of internal capture coroutines and DAC buffer flush on the Host side.
   - Forceful `MediaProjection` termination to prevent background audio capture leakage.

---

### 📦 Installation
Download and install `LaSync.apk` directly on all Android devices (Android 8.0+ / API 26+).
