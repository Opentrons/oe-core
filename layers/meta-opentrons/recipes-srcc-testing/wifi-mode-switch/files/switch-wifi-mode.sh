#! /bin/bash

PROFILE_ENV="/etc/profile.d/wifi-mode.sh"
WIFI_CONFIG="/etc/modprobe.d/toradex-wifi-config.conf"

print_menu() {
	cat <<-EOF
	Select Wifi Mode
	0: Exit
	1: Normal Mode - iperf, nmcli, etc
	2: MFG Mode    - labtool
	EOF
}

main() {
	# print options and get user input
	print_menu
	read INPUT
	case $INPUT in
		1)
			echo "Setting WIFI Normal Mode"
			cat <<-EOF > $WIFI_CONFIG
			blacklist mlan bt8xxx
			install mlan /bin/false
			install bt8xxx /bin/false
			EOF

			# set profile.d
			cat <<-EOF > $PROFILE_ENV
			echo "Entered Normal Wifi Mode"
			EOF
			;;
		2)
			echo "Setting WIFI MFG Mode"
			cat <<-EOF > $WIFI_CONFIG
			blacklist mwifiex mwifiex_sdio btmrvl btmrvl_sdio
			install mwifiex /bin/false
			install btmrvl /bin/false
			EOF

			# set profile.d
			cat <<-EOF > $PROFILE_ENV
			echo "Entered MFG Wifi Mode"
			EOF
			;;
		*)
			echo "Cancelled"
			return 0;
	esac

	echo "Rebooting the device to apply settings"
	reboot -f
}

main "$@"
