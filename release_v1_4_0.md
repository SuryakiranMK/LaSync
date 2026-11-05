# 🎵 LaSync v1.4.0: "PhaseLock" — Acoustic Auto-Calibration & Official Apache 2.0 Licensing

**Release Version:** `v1.4.0`  
**Build Number:** `17`  
**Codename:** `PhaseLock`  
**License:** `Apache License 2.0`  
**Release Date:** November 5, 2026  

---

## 🌟 Executive Summary

**LaSync v1.4.0 ("PhaseLock")** introduces **Acoustic Auto-Calibration**—a breakthrough capability that utilizes smartphone microphones and DSP matched filtering to measure physical room acoustic propagation delay and automatically phase-lock speakers down to sub-millisecond precision.

This release also establishes **official open-source licensing** for the LaSync codebase under the **Apache License, Version 2.0**, providing clear intellectual property protection, patent grant protections, and open distribution rights.

---

## ⚡ What's New in v1.4.0

### 1. 🎙️ PhaseLock Acoustic Auto-Calibration (`AcousticCalibrationEngine.kt`)
* **Linear Frequency Chirp Emission**: The Host generates an optimized 60ms acoustic sweep (1,500 Hz to 3,500 Hz) with a Hann-window envelope to eliminate speaker pop and low-frequency room rumble.
* **DSP Matched Filter Cross-Correlation**: Listener microphones capture room sound and compute normalized cross-correlation against the reference template:
  $$R[n] = \sum_{k=0}^{M-1} s_{\text{mic}}[n+k] \cdot c_{\text{template}}[k]$$
* **Automated Delay Trim Calculation**: Compares the physical acoustic arrival sample against the NTP-synchronized presentation clock, deriving physical speaker distance + audio HAL driver latency.
* **Zero-Touch Acoustic Lock**: Automatically sets the receiver's `delayTrimMs` (-250ms to +350ms) and reports calibration metrics back to the Host Soundboard.

### 2. 📄 Official Open-Source Licensing (Apache License 2.0)
* Added official `LICENSE` file under the industry-standard **Apache License, Version 2.0**.
* Standardized open-source attribution, commercial use permissions, and patent grant coverage.
* Added interactive GitHub release and license status badges to `README.md`.

### 3. 🎛️ Host Soundboard Auto-Calibrate Console
* **One-Tap Calibration Trigger**: Added `[ 🎙️ AUTO-CALIBRATE ]` control to the Host Soundboard Mixer Console header.
* **Live Status Telemetry**: Displays animated pulsing equalizer while chirp sequence is emitting and propagating.
* **Synchronized Reporting**: Automatically reconciles receiver channel strips when listener devices report calibrated millisecond offsets.

### 4. 🎧 Receiver PhaseLock Card
* **Modular Calibration Card**: Prominently displays acoustic alignment status (`Idle` → `Listening` → `Phase-Locked` or `Failed`).
* **Confidence Scoring**: Displays correlation confidence percentage (e.g., `✅ Phase-Locked! Calibrated +16ms delay (96% confidence)`).
* **Manual & Auto Modes**: Listeners can initiate calibration independently or participate in Host-wide sweeps.

---

## 🛠️ Verification & Quality Assurance

* **Unit Test Coverage**: 33 unit tests covering DSP cross-correlation, Hann envelope tapering, packet serialization, and NTP jitter filtering passed with 100% green status.
* **Hardware Compatibility**: Verified on Android 14 (API 34) and Android 15 (API 35) with low-latency `AudioTrack` and `AudioRecord`.
* **Zero ART Regressions**: Modular subcomponent composables guarantee bytecode register count remains well within ART verifier limits.

---

## 📦 Download & Verification

* **APK File:** [`LaSync.apk`](LaSync.apk)
* **SHA-256:** `5C9C4BC34518557846A29D075CAAE81237CDEB322F6A19913961D16F4E20A549`
* **Size:** `17.7 MB`
