inherit get_ot_system_version systemd

# Copyright (C) 2023 Seth Foster <seth@opentrons.com>
# Released under the MIT License (see COPYING.MIT for the terms)
DESCRIPTION = "installs defaults for the remote shell user environment"
LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/Apache-2.0;md5=89aea4e17d99a7cacdbeed46a0096b10"

SRC_URI = "file://ot-environ.sh file://opentrons-run-boot-scripts.service file://opentrons_simulate file://opentrons_execute"

do_install() {
	install -d ${D}/${sysconfdir}/profile.d/
	install -m 0755 ${WORKDIR}/ot-environ.sh ${D}/${sysconfdir}/profile.d/ot-environ.sh
    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${WORKDIR}/opentrons-run-boot-scripts.service ${D}${systemd_system_unitdir}/opentrons-run-boot-scripts.service
    install -d ${D}/${bindir}
    install -m 0666 ${WORKDIR}/opentrons_simulate ${D}${bindir}/opentrons_simulate
    install -m 0666 ${WORKDIR}/opentrons_execute ${D}${bindir}/opentrons_execute
	# add the openembedded version to ot-environ file
	echo "export OT_SYSTEM_VERSION=${OT_SYSTEM_VERSION}" >> ${D}/${sysconfdir}/profile.d/ot-environ.sh
}

addtask do_get_oe_version after do_compile before do_install
do_install[prefuncs] += "do_get_oe_version"

FILES:${PN} += " ${sysconfdir}/profile.d/ot-environ.sh ${systemd_system_unitdir}/opentrons-run-boot-scripts.service ${bindir}/opentrons_simulate ${bindir}/opentrons_execute"

SYSTEMD_AUTO_ENABLE = "enable"
SYSTEMD_SERVICE:${PN} = "opentrons-run-boot-scripts.service"
