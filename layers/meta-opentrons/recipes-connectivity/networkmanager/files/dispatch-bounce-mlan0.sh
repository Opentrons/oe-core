#!/usr/bin/env sh

# Detect when the wifi interface was downed because the driver crashed and reload it

test "$NM_DISPATCHER_ACTION" = "down" || exit 0
test "$DEVICE_IFACE" = "mlan0" || exit 0

test $(nmcli dev show mlan0) -eq 0 && exit 0
echo 'Detected mlan0 crash, unbinding/rebinding'

echo 30b60000.mmc > /sys/bus/platform/drivers/sdhci-esdhc-imx/unbind
echo 30b60000.mmc > /sys/bus/platform/drivers/sdhci-esdhc-imx/bind

echo 'Restarting NetworkManager after mlan0 crash'
systemctl restart NetworkManager
