
inherit externalsrc
EXTERNALSRC = "${@os.path.abspath(os.path.join("${TOPDIR}", os.pardir, os.pardir, "opentrons"))}"

LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://LICENSE;md5=3b83ef96387f14655fc854ddc3c6bd57"

# Modify these as desired
DEST_SYSTEMD_DROPFILE ?= "${B}/system-server-version.conf"
OT_PACKAGE = "system-server"

# Rust python modules installed by pip get stripped outside OE infra
INSANE_SKIP:${PN}:append = "already-stripped"

inherit systemd get_ot_package_version

SYSTEMD_AUTO_ENABLE = "enable"
SYSTEMD_SERVICE:${PN} = "opentrons-system-server.service"
FILESEXTRAPATHS:prepend = "${THISDIR}/files:"
SRC_URI:append = " file://opentrons-system-server.service"

OPENTRONS_APP_BUNDLE_PROJECT_ROOT = "${S}/system-server"
OPENTRONS_APP_BUNDLE_DIR = "/opt/opentrons-system-server"
OPENTRONS_APP_BUNDLE_USE_GLOBAL = "systemd-python "
OPENTRONS_APP_BUNDLE_STRIP_HASHES = "yes"
OPENTRONS_APP_BUNDLE_EXTRA_PIP_ENVARGS_LOCAL = "OPENTRONS_PROJECT=${OPENTRONS_PROJECT} ${@get_ot_package_version_override(d)}"
OPENTRONS_APP_BUNDLE_PACKAGE_SOURCE = "uv"

do_compile:append() {
}

addtask do_write_systemd_dropfile after do_compile before do_install

do_install:append () {
    # create json file to be used in VERSION.json
    install -d ${D}/opentrons_versions
    python3 ${S}/scripts/python_build_utils.py system-server ${OPENTRONS_PROJECT} dump_br_version > ${D}/opentrons_versions/opentrons-system-server-version.json

    install -d ${D}/${systemd_system_unitdir}
    install -m 0644 ${WORKDIR}/opentrons-system-server.service ${D}/${systemd_system_unitdir}/opentrons-system-server.service
}

FILES:${PN}:append = " ${systemd_system_unitdir/opentrons-system-server.service.d \
                       ${systemd_system_unitdir}/opentrons-system-server.service.d/system-server-version.conf \
                       "

RDEPENDS:${PN} += " python3-pyjwt nginx python3-systemd"

DEPENDS += " cargo-native "

inherit opentrons_app_bundle
