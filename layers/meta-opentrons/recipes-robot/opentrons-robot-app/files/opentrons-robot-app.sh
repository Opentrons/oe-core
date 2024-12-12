#!/bin/sh
if test -z "$XDG_RUNTIME_DIR"; then
    export XDG_RUNTIME_DIR=/run/user/`id -u`
    if ! test -d "$XDG_RUNTIME_DIR"; then
        mkdir --parents $XDG_RUNTIME_DIR
        chmod 0700 $XDG_RUNTIME_DIR
    fi
fi

# wait for weston
while [ ! -e  $XDG_RUNTIME_DIR/wayland-0 ] ; do sleep 0.1; done
sleep 1

/opt/opentrons-app/opentrons \
    --disable-gpu \
    --remote-allow-origins=* \
    --remote-debugging-port=9222 \
    --discovery.candidates=localhost \
    --discovery.ipFilter=\"127.0.0.1\" \
    --isOnDevice=1 \
    --no-sandbox \
    --enable-features=UseOzonePlatform \
    --ozone-platform=wayland \
    --in-process-gpu \
    --disable-software-rasterizer \
    --python.pathToPythonOverride=/usr/bin/python3\
