#!/usr/bin/env bash
set -Eeuo pipefail

WG_IF="wg0"
WG_PORT="51820"
WG_NET="10.66.66.0/24"
SERVER_ADDR="10.66.66.1/24"
CLIENT_ADDR="10.66.66.2/32"
SERVER_CONF="/etc/wireguard/${WG_IF}.conf"
CLIENT_OUT="/root/myvpn-client.conf"

if [[ "${EUID}" -ne 0 ]]; then
  echo "Run as root: sudo bash $0" >&2
  exit 1
fi

if [[ ! -r /etc/os-release ]] || ! . /etc/os-release || [[ "${ID:-}" != "ubuntu" ]]; then
  echo "This bootstrap script currently supports Ubuntu only." >&2
  exit 1
fi

if [[ -e "$SERVER_CONF" ]]; then
  echo "$SERVER_CONF already exists. Refusing to overwrite an existing VPN." >&2
  exit 1
fi

echo "=== My VPN / WireGuard server bootstrap ==="
read -r -p "VPS public IPv4 address or DNS name: " ENDPOINT
if [[ -z "$ENDPOINT" || "$ENDPOINT" == *" "* ]]; then
  echo "A valid public endpoint is required." >&2
  exit 1
fi

DEFAULT_IF="$(ip -4 route list default | awk '{print $5; exit}')"
read -r -p "Public network interface [${DEFAULT_IF}]: " WAN_IF
WAN_IF="${WAN_IF:-$DEFAULT_IF}"
if [[ -z "$WAN_IF" ]] || ! ip link show "$WAN_IF" >/dev/null 2>&1; then
  echo "Could not find network interface: $WAN_IF" >&2
  exit 1
fi

export DEBIAN_FRONTEND=noninteractive
apt-get update
apt-get install -y wireguard iptables

umask 077
install -d -m 700 /etc/wireguard
SERVER_PRIV="$(wg genkey)"
SERVER_PUB="$(printf '%s' "$SERVER_PRIV" | wg pubkey)"
CLIENT_PRIV="$(wg genkey)"
CLIENT_PUB="$(printf '%s' "$CLIENT_PRIV" | wg pubkey)"

cat > "$SERVER_CONF" <<EOF
[Interface]
Address = $SERVER_ADDR
ListenPort = $WG_PORT
PrivateKey = $SERVER_PRIV
SaveConfig = false
PostUp = iptables -t nat -A POSTROUTING -s $WG_NET -o $WAN_IF -j MASQUERADE; iptables -A FORWARD -i %i -j ACCEPT; iptables -A FORWARD -o %i -j ACCEPT
PostDown = iptables -t nat -D POSTROUTING -s $WG_NET -o $WAN_IF -j MASQUERADE; iptables -D FORWARD -i %i -j ACCEPT; iptables -D FORWARD -o %i -j ACCEPT

[Peer]
PublicKey = $CLIENT_PUB
AllowedIPs = $CLIENT_ADDR
EOF
chmod 600 "$SERVER_CONF"

cat > "$CLIENT_OUT" <<EOF
[Interface]
PrivateKey = $CLIENT_PRIV
Address = $CLIENT_ADDR
DNS = 1.1.1.1, 1.0.0.1

[Peer]
PublicKey = $SERVER_PUB
Endpoint = $ENDPOINT:$WG_PORT
AllowedIPs = 0.0.0.0/0
PersistentKeepalive = 25
EOF
chmod 600 "$CLIENT_OUT"

cat > /etc/sysctl.d/99-my-vpn.conf <<'EOF'
net.ipv4.ip_forward=1
EOF
sysctl --system >/dev/null

systemctl enable --now "wg-quick@${WG_IF}"

echo
echo "WireGuard server started."
echo "Interface: $WG_IF"
echo "Listen port: UDP $WG_PORT"
echo "WAN interface: $WAN_IF"
echo
echo "IMPORTANT: allow inbound UDP $WG_PORT in your VPS provider firewall."
if command -v ufw >/dev/null 2>&1 && ufw status | grep -q "Status: active"; then
  echo "UFW is active. Run: ufw allow $WG_PORT/udp"
fi
echo "Client profile saved to: $CLIENT_OUT"
echo "Transfer that file securely to your Android phone. Never commit it to Git."
echo
wg show "$WG_IF"
