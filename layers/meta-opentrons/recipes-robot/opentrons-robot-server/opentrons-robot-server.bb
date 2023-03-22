
inherit externalsrc
EXTERNALSRC = "${@os.path.abspath(os.path.join("${TOPDIR}", os.pardir, os.pardir, "opentrons"))}"

LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://LICENSE;md5=3b83ef96387f14655fc854ddc3c6bd57"

# Modify these as desired
DEST_SYSTEMD_DROPFILE ?= "${B}/robot-server-version.conf"
OT_PACKAGE = "robot-server"
inherit insane systemd get_ot_package_version

SYSTEMD_AUTO_ENABLE = "enable"
SYSTEMD_SERVICE_${PN} = "opentrons-robot-server.service opentrons-ot3-canbus.service"
FILESEXTRAPATHS_prepend = "${THISDIR}/files:"
SRC_URI_append = " file://opentrons-robot-server.service file://opentrons-ot3-canbus.service file://95-opentrons-modules.rules"

PIPENV_APP_BUNDLE_PROJECT_ROOT = "${S}/robot-server"
PIPENV_APP_BUNDLE_DIR = "/opt/opentrons-robot-server"
PIPENV_APP_BUNDLE_USE_GLOBAL = "numpy systemd-python python-can wrapt pyzmq "
PIPENV_APP_BUNDLE_STRIP_HASHES = "yes"
PIPENV_APP_BUNDLE_EXTRA_PIP_ENVARGS = "OPENTRONS_PROJECT=ot3"

do_compile_append() {
    # dont include scripts
    rm -rf ${PIPENV_APP_BUNDLE_SOURCE_VENV}/opentrons/resources/scripts
}

addtask do_write_systemd_dropfile after do_compile before do_install

do_install_append () {
    # add release notes
    install -d ${D}${sysconfdir}
    # TODO: Make this not the internal release notes at some point
    install ${S}/api/release-notes-internal.md ${D}${sysconfdir}/release-notes-internal.md

    # create json file to be used in VERSION.json
    install -d ${D}/opentrons_versions
    python3 ${S}/scripts/python_build_utils.py robot-server ot3 dump_br_version > ${D}/opentrons_versions/opentrons-robot-server-version.json

    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${WORKDIR}/opentrons-robot-server.service ${D}${systemd_system_unitdir}/opentrons-robot-server.service
    install -d ${D}${systemd_system_unitdir}/opentrons-robot-server.service.d
    install -m 0644 ${B}/robot-server-version.conf ${D}${systemd_system_unitdir}/opentrons-robot-server.service.d/robot-server-version.conf
    install -m 0644 ${WORKDIR}/opentrons-ot3-canbus.service ${D}${systemd_system_unitdir}/opentrons-ot3-canbus.service
    install -d ${D}${sysconfdir}/udev/rules.d/
    install -m 0644 ${WORKDIR}/95-opentrons-modules.rules ${D}${sysconfdir}/udev/rules.d/95-opentrons-modules.rules
}

FILES_${PN}_append = " ${systemd_system_unitdir/opentrons-robot-server.service.d \
                       ${systemd_system_unitdir}/opentrons-robot-server.service.d/robot-server-version.conf \
                       ${sysconfdir}/udev/rules.d/95-opentrons-modules.rules \
                       ${sysconfdir}/release-notes.md \
                       "

RDEPENDS_${PN} += " udev python3-numpy python3-systemd nginx python-can python3-pyzmq libgpiod-python python-aionotify"

inherit pipenv_app_bundle
