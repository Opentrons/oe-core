#!/usr/bin/env bash
set -euo pipefail


dhcpd -4 -pf /run/dhcp-server/dhcpd.pid -cf /etc/dhcpd/dhcpd.conf ${1}
in.tftpd -4 -l -a 0.0.0.0 -s /provide/boot
/usr/local/bin/entrypoint.sh
