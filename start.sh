#! /bin/bash

# Wrapper for bitbake and openembedded builds usable both inside
# and outside a docker container.
#
# One mandatory argument that provides the top directory (required
# for docker support). Run like
# ./start.sh .
#
# or, from some other directory, /path/to/this/file/start.sh
#
# Any subsequent arguments are passed to the main bitbake invocation,
# and will replace the default target if they are present
set -x

cleanup () {
  popd >/dev/null
}

DEFAULT_TARGET=opentrons-ot3-image
THISDIR="${1}"
shift
TARGET="${1:-${DEFAULT_TARGET}}"
shift

trap cleanup EXIT

sudo chown -hR $USER_NAME:$USER_NAME /volumes && chmod -R ug+rw /volumes

pushd ${THISDIR}

export BITBAKEDIR=${THISDIR}/tools/bitbake
. layers/openembedded-core/oe-init-build-env ${THISDIR}/build

# Download locations are being ignored and we are running out of space, so
# for now just create a symlink from /volumes/cache to ~/.cache which is
# externally mounted to S3.
mkdir -p /volumes/cache/
mkdir -p ~/.cache/
ln -sf /volumes/cache ~/.cache

BB_NUMBER_THREADS=$(nproc) bitbake ${TARGET} "$@"
exit $?
