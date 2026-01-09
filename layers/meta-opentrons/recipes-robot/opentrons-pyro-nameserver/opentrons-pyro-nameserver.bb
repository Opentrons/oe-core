SUMMARY = "Opentrons nameserver application for Python Remote Objects."
DESCRIPTION = "This will start the Pyro5 nameserver for inter-process communication."
LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/Apache-2.0;md5=89aea4e17d99a7cacdbeed46a0096b10"

inherit allarch systemd

RDEPENDS:${PN} = "python3-pyro5"

S = "${WORKDIR}"

SRC_URI = " \
    file://opentrons-pyro-nameserver.service \
"

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

do_install:append () {
    install -m 0644 ${WORKDIR}/opentrons-pyro-nameserver.service ${D}${systemd_unitdir}/system
}

FILES:${PN} += " ${systemd_unitdir}/system/opentrons-pyro-nameserver.service "

SYSTEMD_PACKAGES = "${PN}"
SYSTEMD_SERVICE:${PN} = "opentrons-pyro-nameserver.service"
