
inherit externalsrc
EXTERNALSRC = "${@os.path.abspath(os.path.join("${TOPDIR}", os.pardir, os.pardir, "opentrons"))}"

LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://LICENSE;md5=3b83ef96387f14655fc854ddc3c6bd57"


RDEPENDS_${PN} += " nginx python3-pyudev python3-pyserial"

# Modify these as desired
PV = "1.0+git${SRCPV}"
SRCREV = "${AUTOREV}"

inherit insane systemd

SYSTEMD_AUTO_ENABLE = "enable"
SYSTEMD_SERVICE_${PN} = "opentrons-usb-bridge.service"
FILESEXTRAPATHS_prepend = "${THISDIR}/files:"
SRC_URI_append = " file://opentrons-usb-bridge.service"

S = "${WORKDIR}/git"
B = "${WORKDIR}/build"

PIPENV_APP_BUNDLE_PROJECT_ROOT = "${S}/usb-bridge"
PIPENV_APP_BUNDLE_DIR = "/opt/ot3usb"
PIPENV_APP_BUNDLE_STRIP_HASHES = "yes"
PIPENV_APP_BUNDLE_EXTRAS = ""
PIPENV_APP_BUNDLE_USE_GLOBAL = "pyudev pyserial"

do_install_append() {
  # create json file to be used in VERSION.json
  install -d ${D}/opentrons_versions
  python3 ${S}/scripts/python_build_utils.py usb-bridge ot3 dump_br_version > ${D}/opentrons_versions/opentrons-usb-bridge-version.json

  install -d ${D}/${systemd_system_unitdir}
  install -m 0644 ${WORKDIR}/opentrons-usb-bridge.service ${D}/${systemd_system_unitdir}/opentrons-usb-bridge.service
}

inherit pipenv_app_bundle