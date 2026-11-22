# Ubuntu WireGuard server

Tested design target: Ubuntu Server 22.04 / 24.04 on a VPS with a public IPv4 address.

Run as root:

```bash
sudo bash setup-wireguard.sh
```

The script installs WireGuard, generates server and client keypairs, enables forwarding, configures NAT, enables `wg-quick@wg0`, and writes the client profile to `/root/myvpn-client.conf`.

Open UDP 51820 in the cloud firewall/security group. If UFW is enabled, allow UDP 51820 there as well.

Keep `/root/myvpn-client.conf` private. It includes a client private key. Do not upload it to GitHub or send it in public chat.

This script creates one server and one client peer for initial testing. For production, use separate client keypairs, revocation, monitoring, backups, patching, and a tested firewall policy.
