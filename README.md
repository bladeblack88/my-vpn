# My VPN — serverless Android Anti-DPI

My VPN is being switched to a **local Anti-DPI mode** for Android. It does not require a VPS, rented server, server subscription, WireGuard keys, or root access.

## How it works

The Android app uses the open-source [ByeDPIAndroid](https://github.com/dovecoteescapee/ByeDPIAndroid) engine. It starts an on-device Android VPN interface and routes traffic through a local ByeDPI SOCKS proxy. Traffic then leaves through the phone's ordinary internet connection.

**This is not a remote VPN:**
- it does not change or hide your public IP address;
- it does not create an encrypted tunnel to a remote VPN server;
- it can help with some DPI-based restrictions, but cannot guarantee that YouTube, Telegram, or every blocked service will work on every provider/network;
- Android's VPN permission prompt is required.

## Source and licensing

The engine is included as a pinned Git submodule at `third_party/ByeDPIAndroid`, with its own native dependencies pinned recursively. The upstream project is licensed under GPL-3.0; see its `LICENSE` file and preserve the license and notices when redistributing builds. The native ByeDPI and hev-socks5-tunnel submodules have their own license terms, which must also be respected.

The previous WireGuard prototype remains in `android/` and the old VPS setup scripts remain in `server/` as historical code. **They are not used by the serverless APK build.** Do not create or rent a server for this mode.

## Build APK

GitHub Actions builds the APK on every push to `main` and on manual workflow runs.

1. Open the [Actions tab](https://github.com/bladeblack88/my-vpn/actions).
2. Select the latest successful **Android APK** workflow run.
3. Download the artifact named `my-vpn-serverless-debug-apk`.
4. Install the APK on Android and approve the VPN permission prompt.

The workflow checks out the pinned source and native submodules, installs JDK 17, Android SDK, NDK and CMake, brands the package as `com.myvpn.app` / **My VPN**, and builds the debug APK.

## Important limitations

- A local DPI bypass is not a full VPN and does not provide IP masking or remote-server encryption.
- DPI strategies are network-dependent. If a provider changes filtering, settings or the engine may need updating.
- Android allows only one active VPN service at a time. Stop other VPN, firewall, or DNS-filter apps before starting My VPN.
- This repository currently builds the upstream ByeDPIAndroid app under the My VPN package/label. The custom Compose interface from the earlier WireGuard prototype has not yet been ported into the local engine.
