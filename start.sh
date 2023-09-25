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
patch -f ./layers/meta-jupyter/conf/layer.conf ./meta-jupyter-backport.patch

export BITBAKEDIR=${THISDIR}/tools/bitbake
. layers/openembedded-core/oe-init-build-env ${THISDIR}/build

# electron is ignoring the cache download set by the electron_config_cache env var
# so for now lets manually create a symlink and set its download location to /volumes/cache
mkdir -p /volumes/cache/electron
mkdir -p /volumes/cache/yarn
mkdir -p /volumes/cache/pip
mkdir -p ~/.cache/
ln -sf /volumes/cache/electron ~/.cache/electron
ln -sf /volumes/cache/yarn ~/.cache/yarn
ln -sf /volumes/cache/pip ~/.cache/pip

BB_NUMBER_THREADS=$(nproc) bitbake ${TARGET} "$@"
exit $?
