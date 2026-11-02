# LaSync v1.0.8 Release Notes 🎛️🔊

## 🌟 What's New in v1.0.8: "SoundBoard" (Host Multi-Device Mixer Console)

Take complete studio control over every speaker and phone in the room! `v1.0.8` turns the Host dashboard into a professional **SoundBoard Studio Console**, allowing the Host to manage different receiver phones with varying latencies, acoustic positions, and volume levels in real time.

---

### 1. Per-Device Delay Trim (`+/- ms`)
* **Room & Bluetooth Echo Elimination**: Every phone has different physical driver latency and room positioning. Host can micro-tune delay per device using quick-stepper controls (`-10ms`, `-5ms`, `RESET`, `+5ms`, `+10ms`).
* **Bluetooth Latency Compensation**: Bluetooth speakers and headphones introduce ~100–200ms codec buffer latency. Host can advance or delay playback per phone from `-250ms` to `+350ms` so all speakers hit the air in flawless acoustic unison.

---

### 2. Auto-Detection of Output Hardware (🎧 vs 🔊)
* **Real-Time Route Telemetry**: Receivers automatically detect their active hardware output sink via Android's `AudioDeviceInfo` and report it in telemetry beacons.
* **Console Hardware Badges**:
  - `[ 🎧 BT AUDIO ]` in Ultraviolet glow (Bluetooth A2DP / LE Audio)
  - `[ 🔊 SPEAKER ]` in Quantum Mint glow (Built-in phone speakers)
  - `[ 🎧 WIRED ]` or `[ 🔌 USB DAC ]` in HyperCyan glow

---

### 3. Remote Volume Faders, Mute, Solo & Drop
* **Per-Phone Remote Volume**: Fine-tune volume levels (0% to 100%) per device directly from the Host screen using `[ -10% ]` and `[ +10% ]` steppers.
* **Studio MUTE & SOLO Controls**:
  - **MUTE**: Remotely silence a single phone without disconnecting it.
  - **SOLO**: Instantly isolate a chosen phone for acoustic checks while muting all other phones in the room with one tap! Tap again to restore all channels.
* **Remote DROP / Kick**: Gracefully disconnect any individual phone from the session.

---

### 4. Interactive Channel Strip UI
* **Upgraded Receiver Cards**: Transformed from passive telemetry blocks into full cyber-acoustic mixer channel strips on the Host screen.
* **Receiver-Side Status Badges**: Listener devices display active audio sink badges, host delay trims, and remote volume/mute indicators directly on their dashboard.
