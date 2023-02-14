# Copyright (C) 2023 Seth Foster <seth@opentrons.com>
# Released under the MIT License (see COPYING.MIT for the terms)
DESCRIPTION = "installs defaults for the remote shell user environment"
LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/Apache-2.0;md5=89aea4e17d99a7cacdbeed46a0096b10"

SRC_URI = "file://ot-environ.sh"

do_install() {
      install -d ${D}/${sysconfdir}/profile.d/
      install -m 0755 ${WORKDIR}/ot-environ.sh ${sysconfdir}/init.d/ot-environ.sh
}

FILES_${PN} += "${sysconfdir}/init.d/ot-environ.sh \
            "
