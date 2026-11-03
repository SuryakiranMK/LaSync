# 🚀 LaSync v1.3.0 — HotspotHost (Zero-Router Autonomous Hotspot & Instant QR Connect)

**LaSync v1.3.0 ("HotspotHost")** liberates multi-device synchronized audio playback from physical Wi-Fi routers! Whether you are camping in the woods, on a road trip, hosting an outdoor barbecue, or at a venue with restrictive hotel/guest Wi-Fi that blocks peer-to-peer UDP broadcasts, **HotspotHost** turns your Host device into a fully autonomous, zero-router Wi-Fi hub.

---

### 🌟 What's New in v1.3.0

#### 1. 📡 Autonomous Zero-Router Hotspot Engine
* **Standalone Wi-Fi Creation**: Create a private, local Wi-Fi hotspot directly in-app using Android's sanctioned `WifiManager.startLocalOnlyHotspot` API (API 26–35).
* **Zero Carrier Entitlement Restrictions**: Bypasses carrier tethering blocks and mobile hotspot limits—creates an isolated local-only Wi-Fi cell for nearby audio receivers.
* **Automatic Subnet Binding**: Sockets automatically route across the hotspot gateway (`192.168.43.1:8888`), ensuring zero configuration for the host.

#### 2. 🔲 Cyber-Acoustic QR Code Instant Connect
* **1-Tap Camera Connect**: Tap **"QR CODE"** on the Host screen to display an on-screen ZXing-rendered QR Code encoding standard Wi-Fi credentials (`WIFI:S:...;T:WPA;P:...;;`) and socket metadata.
* **Google Lens & Native Camera Support**: Friends and guests can point their Android or iOS camera directly at your screen to instantly join the hotspot without typing complex passwords.
* **Socket & Credential Clipboard**: Quick-copy buttons for Wi-Fi SSID, Passphrase, and Host Socket IP address directly from the Cyber-Acoustic dialog.

#### 3. ⚡ Receiver 1-Tap Preset (`192.168.43.1`)
* **Instant Hotspot Join**: For listeners joined to the Host's autonomous hotspot, a dedicated **"ON HOST HOTSPOT? 1-TAP JOIN"** card is displayed on the Listener screen.
* **Zero Typing**: Connects directly to `192.168.43.1:8888` with one tap, bypassing LAN discovery delays.

#### 4. 🛡️ Lifecycle & Foreground Persistence
* **Synchronized Hotspot Teardown**: Ending a host session or stopping from notification controls automatically closes the local hotspot reservation, preserving battery life and releasing Wi-Fi hardware.
* **Dual LAN + Hotspot Fallback**: Seamlessly falls back to regular Wi-Fi router mode whenever a home or venue network is connected.

---

### 📦 Installation & Verification
* Download `LaSync.apk` directly from this release, or update inside the app via the In-App GitHub Auto-Updater (`v1.1.2+`).
* Tested & verified on Android 8.0 through Android 15.

