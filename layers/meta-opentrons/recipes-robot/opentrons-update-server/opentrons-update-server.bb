inherit externalsrc

EXTERNALSRC = "${@os.path.abspath(os.path.join("${TOPDIR}", os.pardir, os.pardir, "opentrons"))}"

LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://LICENSE;md5=3b83ef96387f14655fc854ddc3c6bd57"


RDEPENDS:${PN} += " bmaptool libubootenv nginx python3-dbus python3-aiohttp python3-systemd"
OT_PROJECT = 'update-server'

inherit systemd get_ot_package_version

SYSTEMD_AUTO_ENABLE = "enable"
SYSTEMD_SERVICE:${PN} = "opentrons-update-server.service"
FILESEXTRAPATHS:prepend := "${THISDIR}/files:"
SRC_URI += "\
           file://opentrons-update-server.service \
           file://opentrons-robot-signing-key.crt \
           "

S = "${WORKDIR}/git"
B = "${WORKDIR}/build"
PIPENV_APP_BUNDLE_PROJECT_ROOT = "${S}/update-server"
PIPENV_APP_BUNDLE_DIR = "/opt/opentrons-update-server"
PIPENV_APP_BUNDLE_STRIP_HASHES = "yes"
PIPENV_APP_BUNDLE_EXTRAS = ""
PIPENV_APP_BUNDLE_USE_GLOBAL = "python3-aiohttp systemd-python python3-async-timeout"
PIPENV_APP_BUNDLE_EXTRA_PIP_ENVARGS_LOCAL = "OPENTRONS_PROJECT=${OPENTRONS_PROJECT} ${@get_ot_package_version_override(d)}"

do_install:append() {
  # create json file to be used in VERSION.json
  install -d ${D}/opentrons_versions
  python3 ${S}/scripts/python_build_utils.py update-server ${OPENTRONS_PROJECT} dump_br_version > ${D}/opentrons_versions/opentrons-update-server-version.json

  install -d ${D}/${systemd_unitdir}/system
  install -m 0644 ${WORKDIR}/opentrons-update-server.service ${D}/${systemd_unitdir}/system

  # install the signing key, we decide if we keep it in the opentrons-ot3-image recipe
  install -m 600 ${WORKDIR}/opentrons-robot-signing-key.crt ${D}/opentrons_versions/opentrons-robot-signing-key.crt
}

inherit pipenv_app_bundle
