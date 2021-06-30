#! /bin/sh

cleanup () {
  popd
}

trap cleanup EXIT

THISDIR=$(dirname ${0})
pushd ${THISDIR}

patch ./layers/meta-toradex-nxp/recipes-kernel/linux/linux-toradex_5.4-2.3.x.bb ./linux-toradex_5.4-2.3.x.patch

export BITBAKEDIR=${THISDIR}/tools/bitbake

. ./layers/openembedded-core/oe-init-build-env ./build

BB_NUMBER_THREADS=$((`nproc`-1)) bitbake -c tdx-reference-minimal-image
