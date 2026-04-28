#!/bin/bash

# Flags to validate
HARDWARE_SUBPROCESS=$(jq -r '.enableHardwareSubprocess' /data/feature_flags.json)

# Determine if subprocess is enabled
if [ "$HARDWARE_SUBPROCESS" = "true" ]; then
    exit 0   # allow service to start
else
    exit 1   # block service
fi
