inherit externalsrc

EXTERNALSRC = "${@os.path.abspath(os.path.join("${TOPDIR}", os.pardir, os.pardir, "opentrons"))}"

LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://LICENSE;md5=3b83ef96387f14655fc854ddc3c6bd57"


RDEPENDS_${PN} += " bmap-tools libubootenv nginx python3-dbus python3-aiohttp python3-systemd"


inherit insane systemd

SYSTEMD_AUTO_ENABLE = "enable"
SYSTEMD_SERVICE_${PN} = "opentrons-update-server.service"
FILESEXTRAPATHS_prepend = "${THISDIR}/files:"
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
PIPENV_APP_BUNDLE_USE_GLOBAL = "python3-aiohttp systemd-python"
PIPENV_APP_BUNDLE_EXTRA_PIP_ENVARGS = "OPENTRONS_PROJECT=${OPENTRONS_PROJECT}"

do_install_append() {
  # create json file to be used in VERSION.json
  install -d ${D}/opentrons_versions
  python3 ${S}/scripts/python_build_utils.py update-server ot3 dump_br_version > ${D}/opentrons_versions/opentrons-update-server-version.json

  install -d ${D}/${systemd_unitdir}/system
  install -m 0644 ${WORKDIR}/opentrons-update-server.service ${D}/${systemd_unitdir}/system

  if [ -z "${SIGNING_KEY}" ]; then
    bbnote "Installing pubkey to require signed updates"
    install -d ${D}/${sysconfdir}
    install -m 600 ${WORKDIR}/opentrons-robot-signing-key.crt ${D}/${sysconfdir}/opentrons-robot-signing-key.crt
  fi
}

# Only include cert if the signing key is given
FILES_${PN} += "${@bb.utils.contains('SIGNING_KEY', '', '', '${sysconfdir}/ \${sysconfdir}/opentrons-robot-signing-key.crt \', d)}"

inherit pipenv_app_bundle
