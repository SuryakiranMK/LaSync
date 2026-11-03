# 🛡️ LaSync v1.2.1: Android Quality-of-Life & Navigation Suite

LaSync **v1.2.1** brings a comprehensive Quality-of-Life (QoL) and ergonomics upgrade, addressing core navigation issues, accidental session exits, Wi-Fi multicast drop bugs, and Android platform standards.

---

### 🚀 Key Improvements in v1.2.1

* **🔙 Intuitive Back Navigation & Exit Confirmation**:
  * **On-Screen Back Button**: Added a dedicated Navigation Up arrow (`ArrowBack`) to `LaSyncTopBar` across all secondary screens.
  * **Smart `BackHandler` Logic**:
    * Cancels discovery and returns cleanly to `HomeScreen` if you enter "Join Session" without being connected (no longer gets stuck or minimizes the app).
    * Prompts with a Cyber-Acoustic **Session Exit Dialog** if an active broadcast or listener connection is running: choose between `[ 📱 Run in Background ]`, `[ ⏹ Disconnect & Exit ]`, or `[ Cancel ]`.
    * Implements the Android standard **Double-Tap Back to Exit** on the Home Screen (*"Press back again to exit"*), eliminating accidental app exits.

* **🌐 Direct Manual IP Connect**:
  * Added an expandable **"Connect by Direct IP"** card on the Receiver screen for networks where Wi-Fi router AP client isolation blocks UDP discovery broadcasts.
  * Enter any Host IP directly (e.g. `192.168.1.50`) and tap `[ JOIN ]` to connect instantly.

* **📞 Android AudioFocus Management**:
  * Integrated `LaSyncAudioFocusManager` with Android OS audio focus rules.
  * Audio automatically ducks/mutes when phone calls arrive, alarms trigger, or another media app takes focus, and seamlessly restores when the interruption finishes.

* **📶 Wi-Fi MulticastLock Integration**:
  * Acquired `WifiManager.MulticastLock` in `LaSyncForegroundService`.
  * Fixes kernel-level Wi-Fi sleep bugs on Samsung, Xiaomi, and Pixel devices that previously caused UDP broadcast beacons and discovery packets to be dropped when the screen turned off.

* **📱 Edge-to-Edge Safe Insets**:
  * Added `statusBarsPadding()` and `navigationBarsPadding()` so top bars, buttons, and bottom controls are never clipped by camera punch-holes, notches, or system 3-button/gesture bars.

* **📋 Tap-to-Copy Host Address**:
  * Tapping the Master Timeline Socket card on `HostScreen` instantly copies the host address (`IP:8888`) to the clipboard with haptic feedback.

* **📳 Tactile Haptic Feedback**:
  * Added subtle native haptic pulses on primary card clicks, megaphone push-to-talk, and mode changes.

---

### 📦 Installation
Download and install `LaSync.apk` directly on all Android devices (Android 8.0+ / API 26+).
