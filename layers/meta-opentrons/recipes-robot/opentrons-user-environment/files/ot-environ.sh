#!/usr/bin/env sh
export RUNNING_ON_VERDIN=1
export OT_API_FF_enableOT3HardwareController="true"
export PYTHONPATH=$PYTHONPATH:/opt/opentrons-robot-server

# add OT_SYSTEM_VERSION 
if [ -f /etc/OT_SYSTEM_VERSION ]; then
	ot_system_version=$(cat /etc/OT_SYSTEM_VERSION)
	echo "Setting OT_SYSTEM_VERSION=${ot_system_version}"
	export OT_SYSTEM_VERSION="${ot_system_version}"
fi
