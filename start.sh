#! /bin/sh

cleanup () {
  popd
}

trap cleanup EXIT

pushd $(dirname ${0})

source ./export
patch ./layers/meta-toradex-nxp/recipes-kernel/linux/linux-toradex_5.4-2.3.x.bb ./linux-toradex_5.4-2.3.x.patch

cd build
BB_NUMBER_THREADS=$((`nproc`-1)) bitbake -c tdx-reference-minimal-image
