# Copyright (C) 2023 Seth Foster <seth@opentrons.com>
# Released under the MIT License (see COPYING.MIT for the terms)
DESCRIPTION = "installs defaults for the remote shell user environment"
LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/Apache-2.0;md5=89aea4e17d99a7cacdbeed46a0096b10"

SRC_URI = "file://ot-environ.sh"

OT_SYSTEM_VERSION=""

python do_get_oe_version() {
    from subprocess import check_output
    version=check_output(['git', 'describe', '--tags', '--always']).decode().strip()
    if version:
        d.setVar("OT_SYSTEM_VERSION", version)
}

do_install() {
	install -d ${D}/${sysconfdir}/profile.d/
	install -m 0755 ${WORKDIR}/ot-environ.sh ${D}/${sysconfdir}/profile.d/ot-environ.sh

	# add the openembedded version to ot-environ file
	echo "export OT_SYSTEM_VERSION=${OT_SYSTEM_VERSION}" >> ${D}/${sysconfdir}/profile.d/ot-environ.sh
}

addtask do_get_oe_version after do_compile before do_install
do_install[prefuncs] += "do_get_oe_version"

FILES:${PN} += "${sysconfdir}/profile.d/ot-environ.sh"
