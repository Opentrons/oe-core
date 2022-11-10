#! /bin/bash

if [ -z "${BACKGROUND}" ]; then
    RUNSTYLE="-it"
else
    RUNSTYLE="-d"
fi
HEREPATH=$(dirname "$(realpath "${0}")")
tmp_dir=$(mktemp -d -t ci-XXXXXXX)
cp "${HEREPATH}/start.sh" "${tmp_dir}"/
docker build -f "${HEREPATH}/Dockerfile" --build-arg "username=`whoami`" --build-arg "host_uid=`id -u`"   --build-arg "host_gid=`id -g`" --tag "ot3-image:latest" ${tmp_dir}

MONOREPO_PATH=$(realpath "$HEREPATH"/../opentrons)
OE_MOUNT="type=bind,src=$HEREPATH,dst=/volumes/oe-core,consistency=delegated"
MONOREPO_MOUNT="type=bind,src=$MONOREPO_PATH,dst=/volumes/opentrons,consistency=delegated"
docker run ${RUNSTYLE} --rm --mount "$OE_MOUNT" --mount "$MONOREPO_MOUNT"  -- ot3-image:latest "$@"
