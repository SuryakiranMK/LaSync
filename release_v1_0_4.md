# LaSync v1.0.4 Release Notes 🚀⚡

## 🌟 What's New in v1.0.4

### 1. In-App GitHub Auto-Updater ("GitPulse")
Never miss a new release or performance hotfix! LaSync now checks GitHub on launch:
* **Background Check on Boot**: Performs an asynchronous, lightweight query to the GitHub Releases API (`https://api.github.com/repos/SuryakiranMK/LaSync/releases/latest`) using standard Android libraries with zero overhead.
* **Cyber-Acoustic Update Dialog**: If a newer version is released on GitHub, a styled dialog displays:
  - New Version Tag (e.g. `v1.0.5`)
  - Release Title and formatted changelog / release notes
  - **[ DOWNLOAD & UPDATE ]** button: Instantly launches the direct download of `LaSync.apk` via the default browser / download manager.
  - **[ SKIP FOR NOW ]** button: Dismisses the prompt and allows the user to continue using the current session uninterrupted.
* **Offline Resilient**: If the phone is offline or in local hotspot mode without internet, the check silently skips with zero popups or crashes.

---

### 2. Full Android 14 & 15 MediaProjection Stability
* Fully incorporates the `v1.0.3` synchronous Foreground Service lifecycle fix, ensuring smooth, crash-free "Share System Music" streaming on modern devices like iQOO Neo 10 (Android 15) and Redmi Note 12 Pro.

