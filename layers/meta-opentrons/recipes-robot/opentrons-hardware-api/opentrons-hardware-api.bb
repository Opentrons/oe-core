SUMMARY = "Service application for the Opentrons Hardware API."
DESCRIPTION = "This will start the Opentrons Hardware API subprocess."
LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/Apache-2.0;md5=89aea4e17d99a7cacdbeed46a0096b10"

inherit systemd get_ot_package_version useradd

OPENTRONS_APP_BUNDLE_PROJECT_ROOT = "${S}/hardware-api"
OPENTRONS_APP_BUNDLE_DIR = "/opt/opentrons-hardware-api"
OPENTRONS_APP_BUNDLE_USE_GLOBAL = "numpy systemd-python python-can wrapt pyzmq mosquitto python3-pyro5"
OPENTRONS_APP_BUNDLE_STRIP_HASHES = "yes"
OPENTRONS_APP_BUNDLE_EXTRA_PIP_ENVARGS_LOCAL = "OPENTRONS_PROJECT=${OPENTRONS_PROJECT} ${@get_ot_package_version_override(d)}"
OPENTRONS_APP_BUNDLE_PACKAGE_SOURCE = "uv"

S = "${WORKDIR}"

SYSTEMD_AUTO_ENABLE = "enable"
SYSTEMD_SERVICE:${PN} = "opentrons-hardware-api.service"
FILESEXTRAPATHS:prepend = "${THISDIR}/files:"
SRC_URI:append = " file://opentrons-hardware-api.service"

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

do_install:append () {
    install -d ${D}${bindir} ${D}${systemd_unitdir}/system/
    install -m 0644 ${WORKDIR}/opentrons-hardware-api.service ${D}${systemd_unitdir}/system
    install -m 0755 ${WORKDIR}/validate-feature-flags.sh ${D}${bindir}
}

FILES:${PN} += " ${systemd_unitdir}/system/opentrons-hardware-api.service \
                 ${bindir}/validate-feature-flags.sh\
                 "

SYSTEMD_PACKAGES = "${PN}"
SYSTEMD_SERVICE:${PN} = "opentrons-hardware-api.service"