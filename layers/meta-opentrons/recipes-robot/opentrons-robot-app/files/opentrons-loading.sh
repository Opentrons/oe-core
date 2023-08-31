#!/bin/sh

# Get the brightness set as early as possibly to prevent flickers
echo 1 > /sys/class/backlight/backlight/brightness

# Absolute path to the video location
VIDEO=$1

# Graceful cleanup upon CTRL-C
trap "gstd-client pipeline_delete p; exit" SIGHUP SIGINT SIGTERM


PIPELINE="filesrc location=$VIDEO ! decodebin ! videoconvert ! glimagesink render-rectangle=\"<0,0,1024,600>\""

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
