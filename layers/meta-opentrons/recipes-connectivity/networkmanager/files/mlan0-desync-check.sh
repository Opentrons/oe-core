#!/usr/bin/env sh
# Soft-reconnect mlan0 when NetworkManager reports connected but the radio has
# no carrier (field desync). Called by mlan0-desync-check.timer.

set -eu

IFACE="mlan0"

nmcli -t -f DEVICE,STATE device 2>/dev/null | grep -q "^${IFACE}:connected$" || exit 0

carrier=$(cat "/sys/class/net/${IFACE}/carrier" 2>/dev/null || echo 0)
[ "$carrier" = "1" ] && exit 0

conn=$(nmcli -t -f GENERAL.CONNECTION device show "$IFACE" 2>/dev/null | head -n1)
conn=${conn#GENERAL.CONNECTION:}
[ -n "$conn" ] && [ "$conn" != "--" ] || exit 0

echo "Detected ${IFACE} desync, reconnecting $conn"
nmcli -w 30 connection down "$conn" || true
sleep 2
nmcli -w 60 connection up "$conn" || true
