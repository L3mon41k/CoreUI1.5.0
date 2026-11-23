# CoreUI 1.5.0

Initial Android APK foundation.

## Camera
- CameraX rear-camera preview.
- Zoom slider and presets from 1x to 15x.
- APK ABI filters: armeabi-v7a and arm64-v8a.

The effective zoom maximum depends on the camera hardware. If the device reports a maximum below 15x, the app displays that limit rather than claiming unsupported magnification.

## Build
Use JDK 17, Android SDK 35 and Gradle 8.9, then run `gradle assembleDebug`.
