# 🛠️ LaSync v1.1.2 Hotfix — In-App APK Downloader & Instant Package Installer

**LaSync v1.1.2 Hotfix** upgrades the GitPulse auto-updater to a seamless, completely in-app download and installation workflow with live percentage animations and instant package installation.

---

### 🚀 What's New & Fixed in v1.1.2

1. **Direct In-App APK Download**:
   - Tapping **[ DOWNLOAD & UPDATE ]** no longer redirects out of the app to a web browser.
   - The APK package is streamed directly inside LaSync with automatic HTTP redirect resolution across GitHub's release CDN.

2. **Real-Time Download Progress Animation**:
   - Animated Cyber-Acoustic harmonic wave equalizer.
   - Real-time download percentage counter (`0% to 100%`).
   - Downloaded size vs total size tracking (`MB / MB`).

3. **Instant Package Installer**:
   - Once the download completes, the button switches to **[ INSTALL UPDATE ]**.
   - Integrated with Android's secure `FileProvider` and `ACTION_VIEW` package archive intent to update the application seamlessly.

4. **Retry & Fallback Resilience**:
   - Gracefully handles network dropouts during download with one-tap retry or fallback browser options.

---

### 📦 Installation
Download and install `LaSync.apk` directly on your Android device (Android 8.0+ / API 26+).
