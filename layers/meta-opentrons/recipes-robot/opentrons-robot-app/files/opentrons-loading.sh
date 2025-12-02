#!/bin/sh

# This script is responsible for setting the splash screen when the
# system boots up. We export the /var/lib/opentrons-system-server/system.env
# environment file to determine if the system is in OEM Mode, we then
# set the splash screen accordingly.

# Get the brightness set as early as possibly to prevent flickers
if [ -f /sys/class/backlight/backlight/brightness ] ; then
   echo 1 > /sys/class/backlight/backlight/brightness
elif [ -f /sys/class/backlight/backlight-verdin-dsi/brightness ] ; then
   echo 1 > /sys/class/backlight/backlight-verdin-dsi/brightness
else
   echo "Cannot set brightness because brightness hooks don't exist"
fi

# Graceful cleanup upon CTRL-C
trap "gstd-client pipeline_delete p; exit" SIGHUP SIGINT SIGTERM

# check the environment file exists, otherside just default to opentrons loading screen
system_env_file="/var/lib/opentrons-system-server/system.env"
if [ -f $system_env_file ]; then
	echo "Found system environment file: ${system_env_file}"
	export $(grep -v '^#' $system_env_file | xargs | tr -d \'\")
fi

oem_mode_enabled=$OT_SYSTEM_SERVER_oem_mode_enabled
oem_mode_splash_custom=$OT_SYSTEM_SERVER_oem_mode_splash_custom
oem_mode_splash_default="/usr/share/opentrons/oem_mode_default.png"
opentrons_default_splash="/usr/share/opentrons/loading.mp4"
splash_screen_path="${opentrons_default_splash}"
PATTERN='(True|true|1)'
if [[ "${oem_mode_enabled}" =~ $PATTERN ]]; then
	echo "OEM Mode is Enabled"
	if [ -f "${oem_mode_splash_custom}" ]; then
		echo "Custom OEM file is set: ${oem_mode_splash_custom}"
		splash_screen_path=$oem_mode_splash_custom
	else
		echo "Default OEM file is set: ${oem_mode_splash_default}"
		splash_screen_path=$oem_mode_splash_default
	fi
fi

# Make sure the file exists
if [ ! -f "${splash_screen_path}" ]; then
	echo "ERROR: Splash screen file not found: ${splash_screen_path}"
	exit 0
fi

echo "Setting the splash screen to: ${splash_screen_path}"
# Render PNG if this is a custom splash, otherwise render opentrons loading video
if [[ "$splash_screen_path" = *.png ]]; then
	echo "rendering PNG"
	PIPELINE="filesrc location=$splash_screen_path ! pngdec ! imagefreeze ! glimagesink render-rectangle=\"<0,0,1024,600>\""
else
	echo "rendering MP4"
	PIPELINE="filesrc location=$splash_screen_path ! decodebin ! videoconvert ! glimagesink render-rectangle=\"<0,0,1024,600>\""
fi

# there is some race condition on startup with weston, gstd, and this
# that means sometimes playing the pipeline fails. by looking for a
# no-error response from the play command we can retry until it succeeds.
while true; do
    # don't hammer the system retrying
    sleep 0.1
    # delete previous pipelines (most likely previous attempts)
    gstd-client pipeline_delete opentronsloading
    gstd-client pipeline_create opentronsloading $PIPELINE
    gstd-client bus_filter opentronsloading eos
    playresponse=$(gstd-client pipeline_play opentronsloading)
    echo $playresponse
    # responses are in json format and have a "code" element that
    # will be 0 on success and non-zero otherwise, perfect to
    # grep for
    echo $playresponse | grep '"code"\s*:\s*0\s*,' && break || echo "Failed to start pipeline, trying again"
done
