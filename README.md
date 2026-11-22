# CodeVideo Studio

Android app for creating animated videos from HTML, CSS, and JavaScript. It runs locally and does not require an AI subscription or a remote rendering service.

## Current features
- HTML/CSS/JavaScript editor with a built-in example.
- Live preview inside Android WebView.
- Canvas-based video export using MediaRecorder.
- Export profiles: 480p, 720p, and 1080p; 24, 30, and 60 FPS; 5–30 seconds.
- Save and reopen HTML projects in the app's private storage.
- Export files are saved as **Downloads/CodeVideoStudio** on Android 10+.

## Important export limitation
The initial renderer records the first HTML `<canvas>` element. CSS-only or DOM-only animations are previewable, but are not captured by this first export implementation. The exported file is WebM, not MP4. Codec availability and real-time rendering speed depend on the device and its Android System WebView version. High resolution and 60 FPS can be demanding on mid-range phones.

## Build an APK with GitHub Actions
1. Open the **Actions** tab in this repository.
2. Allow GitHub Actions if GitHub asks.
3. Open **Build Android APK**.
4. After the workflow finishes successfully, open the run and download the **CodeVideoStudio-debug-apk** artifact.
5. Extract the ZIP and install `app-debug.apk` on Android.

Every push to `main` also starts a build automatically.

## Local build
Requires JDK 17, Android SDK platform 35, and Gradle 8.9.

```bash
gradle :app:assembleDebug
```

The APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.
