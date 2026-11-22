# My VPN

A private WireGuard VPN for Android, with a self-hosted Ubuntu server.

## What is in this repository

- `android/` — Android client (Kotlin + Jetpack Compose) with WireGuard tunnel support.
- `server/` — Ubuntu VPS provisioning script for a single WireGuard server and one client profile.
- `.github/workflows/android.yml` — builds a debug APK on every push and manual run.

## Current MVP scope

This is a first working foundation, not a commercial multi-region VPN service yet. You need an Ubuntu VPS with a public IPv4 address and permission to open UDP port 51820. The setup script creates a server keypair and a client profile locally on the VPS. Import that profile into My VPN.

**Never commit WireGuard private keys or client configuration files to GitHub.** Treat the generated client profile as a password.

## 1. Create the VPN server

Provision an Ubuntu 22.04/24.04 VPS (1 vCPU and 1 GB RAM is enough for initial testing). In the VPS console, run:

```bash
sudo apt-get update
sudo apt-get install -y git
git clone https://github.com/bladeblack88/my-vpn.git
cd my-vpn
sudo bash server/setup-wireguard.sh
```

The script asks for the VPS public IPv4 address and network interface. It creates `wg0`, enables IPv4 forwarding, configures NAT, and writes a client profile to:

```
/root/myvpn-client.conf
```

Allow inbound **UDP 51820** in your VPS provider firewall/security group. If UFW is enabled, allow it there too:

```bash
sudo ufw allow 51820/udp
```

Check the tunnel:

```bash
sudo wg show
sudo systemctl status wg-quick@wg0
```

Copy the client profile securely to your phone (for example with a one-time secure transfer). Do not paste it into public issues, chat rooms, or this repository.

## 2. Build Android APK

Open `android/` in Android Studio (JDK 17, Android SDK 35), or use GitHub Actions. Every push builds a debug APK. Download it from the workflow run's **Artifacts** section; artifact name: `my-vpn-debug-apk`.

To build locally:

```bash
cd android
./gradlew assembleDebug
```

APK output: `android/app/build/outputs/apk/debug/app-debug.apk`.

## 3. Connect

1. Install the APK on Android.
2. Tap **Add WireGuard config** and select `myvpn-client.conf`.
3. Tap **Connect** and approve Android's VPN permission prompt.
4. Confirm a handshake with `sudo wg show` on the VPS.

## Security notes

- The client private key is generated on the server for this initial bootstrap. For stronger operational security, generate the client key on-device and add its public key to the server.
- Keep SSH access key-based; disable password SSH after confirming key access.
- Do not expose the WireGuard UDP port in any place other than required network firewalls.
- This MVP does not yet include account login, billing, remote server discovery, multi-country selection, or a kill switch.
- A VPN does not make a user anonymous. The VPS provider and websites/services still observe relevant connection metadata.

See [server/README.md](server/README.md) and [android/README.md](android/README.md).
