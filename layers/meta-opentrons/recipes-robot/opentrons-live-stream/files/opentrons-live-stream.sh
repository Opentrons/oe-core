#!/bin/sh
# This script is responsible for starting our FFMPEG process.
# The FFMPEG process samples the video source provided by the
# opentrons-live-stream.env file to be streamed at an endpoint.

: "${1:?Error: Current boot id must be set as parameter 1}"; CURRENT_BOOT_ID="$1"
: "${2:?Error: Boot id must be set as parameter 2}"; BOOT_ID="$2"
: "${3:?Error: Status must be set as parameter 3}"; STATUS="$3"
: "${4:?Error: Source must be set as parameter 4}"; SOURCE="$4"
: "${5:?Error: Resolution must be set as parameter 5}"; RESOLUTION="$5"
: "${6:?Error: Framerate must be set as parameter 6}"; FRAMERATE="$6"
: "${7:?Error: Bitrate must be set as parameter 7}"; BITRATE="$7"

if [ "$CURRENT_BOOT_ID" == "$BOOT_ID" ]; then 
  if [ -e "$SOURCE" ] && [ "$STATUS" != "OFF" ]; then
    echo "Beginning Opentrons Live Stream with camera $SOURCE"
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

  else
    echo "Could not begin stream with camera $SOURCE and status $STATUS, exiting."
    exit 3
  fi

else
  echo "Current Boot ID does not match configuration Boot ID, exiting opentrons live stream."
  exit 3
fi