#! /bin/bash

set -x

cleanup () {
  popd >/dev/null
}

trap cleanup EXIT

THISDIR=$(dirname ${0})
pushd ${THISDIR}

patch ./layers/meta-toradex-nxp/recipes-kernel/linux/linux-toradex_5.4-2.3.x.bb ./linux-toradex_5.4-2.3.x.patch

export BITBAKEDIR=${THISDIR}/tools/bitbake
. layers/openembedded-core/oe-init-build-env ${THISDIR}/build

BB_NUMBER_THREADS=$((`nproc`-1)) bitbake opentrons-ot3-image

cd ${THISDIR}
mkdir -p build/deploy/opentrons
cp $(find build/deploy/images/verdin-imx8mm/ | grep opentrons-ot3-image-Tezi | head -n 1) build/deploy/opentrons/opentrons-image.tar
