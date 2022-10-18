# Recipe created by recipetool
# This is the basis of a recipe and may need further editing in order to be fully functional.
# (Feel free to remove these comments when editing.)

# WARNING: the following LICENSE and LIC_FILES_CHKSUM values are best guesses - it is
# your responsibility to verify that the values are complete and correct.
LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://LICENSE;md5=3b83ef96387f14655fc854ddc3c6bd57"

SRC_URI = "git://github.com/Opentrons/opentrons.git;protocol=https;branch=edge;"

RDEPENDS_${PN} += " bmap-tools libubootenv nginx python3-dbus python3-aiohttp python3-systemd"

# Modify these as desired
PV = "1.0+git${SRCPV}"
SRCREV = "${AUTOREV}"

inherit insane systemd

SYSTEMD_AUTO_ENABLE = "enable"
SYSTEMD_SERVICE_${PN} = "opentrons-update-server.service"
FILESEXTRAPATHS_prepend = "${THISDIR}/files:"
SRC_URI_append = " file://opentrons-update-server.service"

S = "${WORKDIR}/git"
B = "${WORKDIR}/build"
PIPENV_APP_BUNDLE_PROJECT_ROOT = "${S}/update-server"
PIPENV_APP_BUNDLE_DIR = "/opt/opentrons-update-server"
PIPENV_APP_BUNDLE_STRIP_HASHES = "yes"
PIPENV_APP_BUNDLE_EXTRAS = ""
PIPENV_APP_BUNDLE_USE_GLOBAL = "python3-aiohttp systemd-python"

do_compile_append() {
  # create json file to be used in VERSION.jso
  python3 ${S}/scripts/python_build_utils.py update-server dump_br_version > ${DEPLOY_DIR_IMAGE}/opentrons-update-server-version.json
}

do_install_append() {
  install -d ${D}/${systemd_unitdir}/system
  install -m 0644 ${WORKDIR}/opentrons-update-server.service ${D}/${systemd_unitdir}/system
}

inherit pipenv_app_bundle
