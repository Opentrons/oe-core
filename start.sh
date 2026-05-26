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

# Writable bind mounts for the build user. Do **not** recurse into oe-core/build/tmp:
# that tree is managed by BitBake/pseudo; blanket chown corrupts pseudo's inode DB
# (e.g. Tezi do_create_tezi_ot3 failing on rm image-json).
OE_VOL="/volumes/oe-core"
sudo chown -hR "${USER_NAME}:${USER_NAME}" /volumes/cache /volumes/opentrons /volumes/ot3-firmware 2>/dev/null || true
sudo chmod -R ug+rw /volumes/cache /volumes/opentrons /volumes/ot3-firmware 2>/dev/null || true
if [ -d "${OE_VOL}" ]; then
  sudo find "${OE_VOL}" -path "${OE_VOL}/build/tmp" -prune -o -exec chown -h "${USER_NAME}:${USER_NAME}" {} +
  sudo find "${OE_VOL}" -path "${OE_VOL}/build/tmp" -prune -o -exec chmod ug+rw {} +
fi

pushd ${THISDIR}

export BITBAKEDIR=${THISDIR}/tools/bitbake
. layers/openembedded-core/oe-init-build-env ${THISDIR}/build

# Download locations are being ignored and we are running out of space, so
# for now just create a symlink from /volumes/cache to ~/.cache which is
# externally mounted to S3.
mkdir -p /volumes/cache/
mkdir -p ~/.cache/
ln -sf /volumes/cache ~/.cache

# CI: run fetch and full image in one container so a second start.sh pass never chowns tmp/work
# between BitBake phases (avoids pseudo inode/path abort on Tezi tasks).
if [ "${OE_CI_FETCH_THEN_BUILD:-}" = "1" ]; then
  BB_NUMBER_THREADS=$(nproc) bitbake "${TARGET}" --runall=fetch || exit $?
fi

BB_NUMBER_THREADS=$(nproc) bitbake "${TARGET}" "$@"
exit $?
