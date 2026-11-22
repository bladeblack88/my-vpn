# Android client

My VPN is an Android WireGuard client built with Kotlin and Jetpack Compose.

## Current functionality

- Imports a WireGuard `.conf` profile using Android's file picker.
- Validates the profile before saving it.
- Encrypts the profile with AES-GCM using a key stored in Android Keystore.
- Connects and disconnects through the official WireGuard tunnel backend.
- Optional auto-connect when the user opens the app, after Android VPN permission has been granted.

## Build

- JDK 17
- Android SDK 35
- Gradle 8.11.1

Open this directory in Android Studio and run `assembleDebug`, or use the GitHub Actions workflow. The workflow publishes a debug APK artifact when the build succeeds.

## Important limitations

This is an early client build, not a hosted VPN service. It does not provision VPN servers, discover trusted free servers, or provide an always-free remote endpoint. A valid WireGuard profile and a reachable server are required.

Auto-connect currently runs when My VPN is opened; it does not yet promise boot-time startup or guaranteed reconnection after every network change. Android requires user approval for VPN access and may restrict background startup. There is no integrated ByeByeDPI mode in this build yet.

Keep the original WireGuard profile private. It contains a private key. The app encrypts its locally stored copy, but importing a profile does not make the remote server itself trustworthy.
