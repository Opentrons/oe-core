#!/bin/bash
# This script is responsible for starting our FFMPEG process.
# The FFMPEG process samples the video source provided by the
# opentrons-live-stream.conf file to be streamed at an endpoint.

FFMPEG_CONFIG="/usr/share/opentrons/opentrons-live-stream.conf"

if [[ -f "$FFMPEG_CONFIG" ]]; then
  source "$FFMPEG_CONFIG"
  echo "Opentrons FFMPEG Configuration loaded from $FFMPEG_CONFIG"
else
  echo "Error: Opentrons FFMPEG Configuration file not found at $FFMPEG_CONFIG"
  exit 1
fi

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