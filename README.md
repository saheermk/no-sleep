<div align="center">
  <img src="logo.webp" width="128" height="128" alt="No Sleep Logo" />
  <h1>No Sleep</h1>
  <p><strong>Silence the Sleep Timer. Keep the Screen Alive.</strong></p>
  <p>A minimalist, high-performance Android utility to keep your device's screen on globally.</p>
</div>

<br />

## What is No Sleep?

**No Sleep** is an elegant Android utility designed to bypass your device's screen timeout settings. Whether you're referencing a recipe, reading long documentation, or monitoring a live process, No Sleep ensures your screen stays active without the need to constantly touch the display.

Built with **Jetpack Compose**, it features a premium UI, smooth animations, and a focus on battery efficiency.

## Key Features

- **Quick Settings Tile**: Toggle No Sleep instantly from your notification panel—no need to open the app.
- **Battery Efficient**: Uses a lightweight 1x1 transparent overlay (`FLAG_KEEP_SCREEN_ON`) instead of power-hungry CPU WakeLocks.
- **Modern Aesthetic**: Built with Material 3, featuring dynamic gradients, ambient glows, and responsive bouncy interactions.
- **Real-time Sync**: State remains perfectly synchronized between the system tile and the dashboard UI.
- **CI/CD Integrated**: Automated build and release pipeline ensures you always get the latest signed production APKs.

## Documentation (A-Z Guide)

For a deep dive into every aspect of this project, including **detailed use cases, technical architecture, and troubleshooting**, please refer to our comprehensive guide:

👉 **[View the Complete Project Documentation (DOCS.md)](./DOCS.md)**

## 🛠 Installation

1. Download the latest **signed APK** from the [Releases](https://github.com/saheermk/no-sleep/releases) page.
2. Install the APK on your Android device (API 26+).
3. **Important**: Grant the "Display over other apps" permission when prompted.

### 💡 Pro Tip

Pull down your Quick Settings panel, tap the **Edit** (pencil) icon, and find the **No Sleep** tile. Drag it into your active tiles for one-tap screen management!

## Permissions

To function correctly, No Sleep requires specific permissions:

- **Display over other apps**: To maintain a 0x0 pixel overlay that natively forces the display to stay awake.
- **Foreground Service**: To prevent the OS from killing the wake lock in the background.
- **Notifications**: To show a persistent status indicator while active.

---

## 👨‍💻 Developer

**Developed by Saheermk**_Creative Developer blending design and engineering to create immersive apps._

- 🌐 **[Website &amp; Portfolio](https://saheermk.pages.dev)**
- 💼 **[LinkedIn](https://in.linkedin.com/in/saheermk)**
- 💻 **[GitHub](https://github.com/saheermk/)**

---

<div align="center">
  <sub>Built with ❤️ and Kotlin / Jetpack Compose</sub>
</div>
