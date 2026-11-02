# LaSync v1.0.7 Release Notes 🎙️📢

## 🌟 What's New in v1.0.7: "GhostMegaphone" (Secret Walkie-Talkie & Room PA System)

Turn your host phone into a stealth 1-way emergency walkie-talkie / Public Address (PA) system! With `v1.0.7`, the Host can broadcast their live microphone audio to every connected phone across the room with forced maximum volume and zero delay.

---

### 1. Stealth Push-to-Talk (PTT) Triggers
* **Stealth Glowing Logo Trigger**: Press and hold the glowing LaSync logo at the top of the Host screen anytime to activate GhostMegaphone. The moment you release your finger, broadcast terminates.
* **Dedicated Cyber-Crimson PA Card**: Interactive "HOLD TO TALK" console button on the Host dashboard with live animated waveform feedback and status indicators.

---

### 2. Live Mic Capture & Instant Audio Ducking
* **Ultra-Low Latency Voice Capture**: Captures 48 kHz 16-bit mono PCM audio directly from the microphone using Android's hardware `VOICE_COMMUNICATION` audio source with built-in acoustic echo cancellation (AEC) and noise suppression.
* **Clean Music Ducking**: Any active system music stream (Spotify, YouTube, games) or acoustic test tone is automatically ducked / muted during live speech transmission so announcements are crystal clear.
* **Instant Un-Ducking**: When the Host releases the PTT trigger, music playback seamlessly resumes.

---

### 3. Emergency Forced Volume Override & Clean Restoration
* **Remote 100% Volume Boost**: Connected receiver phones receive the emergency announcement control trigger (`TYPE_MEGAPHONE_START`) and audio frame packets (`0x06`). `VolumeOverrideManager` automatically records the user's current volume level and forces Android's `STREAM_MUSIC` volume to 100% maximum output so the room can hear the Host immediately.
* **Zero Jitter Delay (Direct Fast-Path)**: Megaphone voice packets bypass the deep 1.0s music buffer, playing immediately through the speaker coils with minimal (<25ms) latency.
* **Automatic Clean Volume Restoration**: When the broadcast ends (`TYPE_MEGAPHONE_STOP` or 2.5s inactivity watchdog), each receiver's volume is immediately restored back to its exact prior level.

---

### 4. Visual Cyber-Alerts on Host & Receiver
* **Host Live Broadcast Indicator**: Glowing Crimson aura around the top bar logo and pulsing wave alert: `🎙️ LIVE MEGAPHONE ACTIVE • TRANSMITTING AT MAX VOLUME`.
* **Receiver Emergency Announcement Banner**: Flashing Crimson emergency banner on listener devices: `🚨 HOST EMERGENCY ANNOUNCEMENT • Volume Overridden to 100%`.
