#!/bin/sh
# This script is responsible for starting our FFMPEG process.
# The FFMPEG process samples the video source provided by the
# opentrons-live-stream.conf file to be streamed at an endpoint.

DEFAULT_SOURCE="NONE"
DEFAULT_RESOLUTION=1280x720
DEFAULT_FRAMERATE=30
DEFAULT_BITRATE=2M

FFMPEG_CONFIG="/var/lib/opentrons-live-stream/opentrons-live-stream.conf"

if [[ -f "$FFMPEG_CONFIG" ]]; then
  source "$FFMPEG_CONFIG"
  echo "Opentrons FFMPEG Configuration loaded from $FFMPEG_CONFIG"

  if [ "$SOURCE" != "NONE" ]; then
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

else
  # Create the configuration file with a default streaming source
  DIRECTORY="/var/lib/opentrons-live-stream"
  if [ ! -d "$DIRECTORY" ]; then
    mkdir -p $DIRECTORY
  fi
  echo -e "SOURCE=$DEFAULT_SOURCE\nRESOLUTION=$DEFAULT_RESOLUTION\nFRAMERATE=$DEFAULT_FRAMERATE\nBITRATE=$DEFAULT_BITRATE" >> $FFMPEG_CONFIG
fi