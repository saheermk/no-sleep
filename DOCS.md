# No Sleep — Project Documentation (A-Z)

Welcome to the comprehensive documentation for **No Sleep**. This guide covers every angle of the project, from technical architecture to real-world usage scenarios.

---

## Table of Contents

1. [Core Philosophy](#core-philosophy)
2. [A-Z Features](#a-z-features)
3. [User Scenarios &amp; Examples](#user-scenarios--examples)
4. [Technical Architecture](#technical-architecture)
5. [Permissions Breakdown](#permissions-breakdown)
6. [Battery &amp; Efficiency](#battery--efficiency)
7. [Troubleshooting &amp; FAQ](#troubleshooting--faq)
8. [Developer Extensions](#developer-extensions)

---

## Core Philosophy

Most "Keep Awake" apps use a CPU-level `WakeLock`, which prevents the device's processor from entering deep sleep, leading to significant battery drain.

**No Sleep** takes a different approach: it leverages the Android `WindowManager` to place an invisible, non-intrusive 1x1 pixel overlay on the screen with the `FLAG_KEEP_SCREEN_ON` flag. This tells the Android Display Manager to keep only the screen active while allowing other system components to manage power normally.

---

## A-Z Features

### **Automatic State Syncing**

The app's internal state is managed via `Kotlin Coroutines StateFlow`. This ensures that if you toggle the state via the Notification Tile, the Main Dashboard updates instantly (and vice versa).

### **Bouncy UI Interactions**

The toggle uses `Spring` physics for its animations. When you flip the switch, it doesn't just move; it reacts with a tactile bounce, making the experience feel premium.

### **Dynamic Gradient Backgrounds**

The entire background shifts from a deep midnight blue to a vibrant "active" navy when the service is running, providing a subtle but clear visual indicator of the app's state.

### **Foreground Service Stability**

To prevent Android from killing the wake lock during memory-intensive tasks, No Sleep runs as a foreground service with a low-importance notification, ensuring persistence.

---

## User Scenarios & Examples

### **The Digital Chef**

_Scenario_: You're following a complex recipe on a website. Your hands are covered in flour or oil.
_Benefit_: With No Sleep active, you can read the next steps without having to touch (and smudge) your screen to wake it up every 30 seconds.

### **The Focused Student**

_Scenario_: You're reading a long research paper or a PDF textbook that requires deep thought.
_Benefit_: No Sleep keeps the page visible even when you're not interacting with the screen for minutes, preventing the disruption of your flow.

### **The On-Device Developer**

_Scenario_: You're monitoring logs or a build process running directly on your Android device (using Termux or similar).
_Benefit_: You can keep the terminal visible to catch errors immediately without the screen timing out.

### **The AFK Gamer**

_Scenario_: You're playing a game that requires long "idle" periods or auto-farming.
_Benefit_: No Sleep ensures the game session stays active and the screen stays on without you needing to prevent timeout manually.

### **The Co-Pilot**

_Scenario_: You're using a navigation app or a custom dashboard on your phone mounted in a car.
_Benefit_: Provides a failsafe to ensure the map stays visible even if the app's internal "keep awake" fails or is misconfigured.

---

## 🏗 Technical Architecture

### **Key Modules**

- **`MainActivity.kt`**: The entry point. Handles the Jetpack Compose UI, permission requests, and user interactions.
- **`WakeLockService.kt`**: The heart of the app. Manages the `SYSTEM_ALERT_WINDOW` overlay and the foreground notification.
- **`ShutterTileService.kt`**: Implements the `TileService` API to provide the Quick Settings toggle.

### **Data Flow**

1. User clicks Toggle (UI or Tile).
2. Command sent to `WakeLockService`.
3. Service updates `isServiceRunning` StateFlow.
4. UI and Tile observe the flow and update their appearance accordingly.

---

## Permissions Breakdown

| Permission              | Why it's required                                                                                                 |
| :---------------------- | :---------------------------------------------------------------------------------------------------------------- |
| `SYSTEM_ALERT_WINDOW` | Critical for the "Invisible Overlay" method. It allows the app to draw over other apps to force the screen state. |
| `FOREGROUND_SERVICE`  | Required on Android 9+ to keep the service running reliably in the background.                                    |
| `WAKE_LOCK`           | Used as a secondary fallback and for system compatibility.                                                        |
| `POST_NOTIFICATIONS`  | Required on Android 13+ to show the status notification.                                                          |

---

## Battery & Efficiency

No Sleep is designed to be "Greener" than standard wake lock tools.

- **No CPU consumption**: It does not keep the CPU "spinning."
- **Low Memory Footprint**: The overlay is a single transparent pixel. All UI components are optimized Jetpack Compose views.

---

## ❓ Troubleshooting & FAQ

### **The screen still turns off!**

- Ensure you have granted the **"Display over other apps"** permission.
- Check if your device has aggressive "Battery Optimization" for No Sleep. Go to **App Info > Battery** and set it to **"Unrestricted"**.

### **Where is the Quick Settings Tile?**

1. Pull down your notification panel twice.
2. Tap the **Pencil (Edit)** icon.
3. Scroll down to "Available Tiles."
4. Find **No Sleep** and drag it to the top.

---

## Developer Extensions

Want to contribute? Here are some ideas:

- **Timer Feature**: Add a countdown to turn off the wake lock automatically.
- **Custom Overlays**: Allow users to change the ambient glow color.
- **Tasker Integration**: Add an intent filter so automation apps can trigger No Sleep.

---

<div align="center">
  **No Sleep — Silence the Sleep Timer.**  
  [GitHub Repository](https://github.com/saheermk/no-sleep)
</div>
