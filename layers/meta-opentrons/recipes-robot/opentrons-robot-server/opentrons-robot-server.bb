
inherit externalsrc
EXTERNALSRC = "${@os.path.abspath(os.path.join("${TOPDIR}", os.pardir, os.pardir, "opentrons"))}"

LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://LICENSE;md5=3b83ef96387f14655fc854ddc3c6bd57"

# Modify these as desired
DEST_SYSTEMD_DROPFILE ?= "${B}/robot-server-version.conf"
OT_PACKAGE = "robot-server"

# Rust python modules installed by pip get stripped outside OE infra
INSANE_SKIP:${PN}:append = "already-stripped"

inherit insane systemd get_ot_package_version

SYSTEMD_AUTO_ENABLE = "enable"
SYSTEMD_SERVICE:${PN} = "opentrons-robot-server.service opentrons-ot3-canbus.service"
FILESEXTRAPATHS:prepend = "${THISDIR}/files:"
SRC_URI:append = " file://opentrons-robot-server.service file://opentrons-ot3-canbus.service file://95-opentrons-modules.rules"

PIPENV_APP_BUNDLE_PROJECT_ROOT = "${S}/robot-server"
PIPENV_APP_BUNDLE_DIR = "/opt/opentrons-robot-server"
PIPENV_APP_BUNDLE_USE_GLOBAL = "numpy systemd-python python-can wrapt pyzmq mosquitto"
PIPENV_APP_BUNDLE_STRIP_HASHES = "yes"
PIPENV_APP_BUNDLE_EXTRA_PIP_ENVARGS_LOCAL = "OPENTRONS_PROJECT=${OPENTRONS_PROJECT} ${@get_ot_package_version_override(d)}"


do_compile:append() {
    # dont include scripts
    rm -rf ${PIPENV_APP_BUNDLE_SOURCE_VENV}/opentrons/resources/scripts
}

addtask do_write_systemd_dropfile after do_compile before do_install

do_install:append () {
    # add release notes
    install -d ${D}${sysconfdir}

    if [ "${OPENTRONS_PROJECT}" == "ot3" ] ; then
       install ${S}/api/release-notes-internal.md ${D}${sysconfdir}/release-notes.md
    else
       install ${S}/api/release-notes.md ${D}${sysconfdir}/release-notes.md
    fi

    # create json file to be used in VERSION.json
    install -d ${D}/opentrons_versions
    python3 ${S}/scripts/python_build_utils.py robot-server ${OPENTRONS_PROJECT} dump_br_version > ${D}/opentrons_versions/opentrons-robot-server-version.json
    python3 ${S}/scripts/python_build_utils.py api ${OPENTRONS_PROJECT} dump_br_version > ${D}/opentrons_versions/opentrons-api-version.json

    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${WORKDIR}/opentrons-robot-server.service ${D}${systemd_system_unitdir}/opentrons-robot-server.service
    install -d ${D}${systemd_system_unitdir}/opentrons-robot-server.service.d
    install -m 0644 ${B}/robot-server-version.conf ${D}${systemd_system_unitdir}/opentrons-robot-server.service.d/robot-server-version.conf
    install -m 0644 ${WORKDIR}/opentrons-ot3-canbus.service ${D}${systemd_system_unitdir}/opentrons-ot3-canbus.service
    install -d ${D}${sysconfdir}/udev/rules.d/
    install -m 0644 ${WORKDIR}/95-opentrons-modules.rules ${D}${sysconfdir}/udev/rules.d/95-opentrons-modules.rules
}

FILES:${PN}:append = " ${systemd_system_unitdir/opentrons-robot-server.service.d \
                       ${systemd_system_unitdir}/opentrons-robot-server.service.d/robot-server-version.conf \
                       ${sysconfdir}/udev/rules.d/95-opentrons-modules.rules \
                       ${sysconfdir}/release-notes.md \
                       "

RDEPENDS:${PN} += " udev python3-numpy python3-systemd nginx python-can python3-pyzmq libgpiod-python python-aionotify mosquitto python-byonoy python3-pyusb"
DEPENDS += " cargo-native "

inherit pipenv_app_bundle
