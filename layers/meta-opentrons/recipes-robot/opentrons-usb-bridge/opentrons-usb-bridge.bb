inherit externalsrc

EXTERNALSRC = "${@os.path.abspath(os.path.join("${TOPDIR}", os.pardir, os.pardir, "opentrons"))}"

LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://LICENSE;md5=3b83ef96387f14655fc854ddc3c6bd57"


RDEPENDS:${PN} += " nginx python3-pyudev python3-pyserial"

OT_PROJECT = 'usb-bridge'

inherit systemd get_ot_package_version

SYSTEMD_AUTO_ENABLE = "enable"
SYSTEMD_SERVICE:${PN} = "opentrons-usb-bridge.service"
FILESEXTRAPATHS:prepend = "${THISDIR}/files:"
SRC_URI:append = " file://opentrons-usb-bridge.service"

S = "${WORKDIR}/git"
B = "${WORKDIR}/build"

OPENTRONS_APP_BUNDLE_PROJECT_ROOT = "${S}/usb-bridge"
OPENTRONS_APP_BUNDLE_DIR = "/opt/ot3usb"
OPENTRONS_APP_BUNDLE_EXTRAS = ""
OPENTRONS_APP_BUNDLE_USE_GLOBAL = "pyudev pyserial"
OPENTRONS_APP_BUNDLE_EXTRA_PIP_ENVARGS_LOCAL = "OPENTRONS_PROJECT=${OPENTRONS_PROJECT} ${@get_ot_package_version_override(d)}"
OPENTRONS_APP_BUNDLE_PACKAGE_SOURCE = "uv"

do_install:append() {
  # create json file to be used in VERSION.json
  install -d ${D}/opentrons_versions
  python3 ${S}/scripts/python_build_utils.py usb-bridge ${OPENTRONS_PROJECT} dump_br_version > ${D}/opentrons_versions/opentrons-usb-bridge-version.json

  install -d ${D}/${systemd_system_unitdir}
  install -m 0644 ${WORKDIR}/opentrons-usb-bridge.service ${D}/${systemd_system_unitdir}/opentrons-usb-bridge.service
}

inherit opentrons_app_bundle
