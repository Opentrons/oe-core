#!/bin/sh
# This script is responsible for starting our FFMPEG process.
# The FFMPEG process samples the video source provided by the
# opentrons-live-stream.conf file to be streamed at an endpoint.

# Capture the current boot ID to determine if stream config is stale
CURRENT_BOOT_ID=$(cat /proc/sys/kernel/random/boot_id)

# Default Configurations
DEFAULT_STATUS="OFF"
DEFAULT_SOURCE="NONE"
DEFAULT_RESOLUTION=1280x720
DEFAULT_FRAMERATE=30
DEFAULT_BITRATE=2000K

FFMPEG_CONFIG="/data/opentrons-live-stream.conf"

if [[ -f "$FFMPEG_CONFIG" ]]; then
  source "$FFMPEG_CONFIG"
  echo "Opentrons FFMPEG Configuration loaded from $FFMPEG_CONFIG"
  if [ "$CURRENT_BOOT_ID" == "$BOOT_ID" ]; then 
    if [ "$SOURCE" != "NONE" ] && [ "$STATUS" != "OFF" ]; then
      ffmpeg \
        -hwaccel auto \
        -video_size $RESOLUTION \
        -i $SOURCE \
        -flags low_delay \
        -c:v h264_v4l2m2m \
        -b:v $BITRATE \
        -f flv \
        -r $FRAMERATE \
        rtmp://localhost/live/stream
    fi
  fi

else
  # Create the configuration file with a default streaming source
  DIRECTORY="/data"
  if [ ! -d "$DIRECTORY" ]; then
    mkdir -p $DIRECTORY
  fi
  echo -e "BOOT_ID=$CURRENT_BOOT_ID\nSTATUS=$DEFAULT_STATUS\nSOURCE=$DEFAULT_SOURCE\nRESOLUTION=$DEFAULT_RESOLUTION\nFRAMERATE=$DEFAULT_FRAMERATE\nBITRATE=$DEFAULT_BITRATE" >> $FFMPEG_CONFIG
fi