
inherit externalsrc
EXTERNALSRC = "${@os.path.abspath(os.path.join("${TOPDIR}", os.pardir, os.pardir, "opentrons"))}"

LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://LICENSE;md5=3b83ef96387f14655fc854ddc3c6bd57"

# Modify these as desired
DEST_SYSTEMD_DROPFILE ?= "${B}/system-server-version.conf"
OT_PACKAGE = "system-server"
inherit insane systemd get_ot_package_version

SYSTEMD_AUTO_ENABLE = "enable"
SYSTEMD_SERVICE_${PN} = "opentrons-system-server.service"
FILESEXTRAPATHS_prepend = "${THISDIR}/files:"
SRC_URI_append = " file://opentrons-system-server.service"

PIPENV_APP_BUNDLE_PROJECT_ROOT = "${S}/system-server"
PIPENV_APP_BUNDLE_DIR = "/opt/opentrons-system-server"
PIPENV_APP_BUNDLE_USE_GLOBAL = "systemd-python "
PIPENV_APP_BUNDLE_STRIP_HASHES = "yes"
PIPENV_APP_BUNDLE_EXTRAS = "./../hardware"

do_compile_append() {
}

addtask do_write_systemd_dropfile after do_compile before do_install

do_install_append () {
    # create json file to be used in VERSION.json
    install -d ${D}/opentrons_versions
    python3 ${S}/scripts/python_build_utils.py system-server ot3 dump_br_version > ${D}/opentrons_versions/opentrons-system-server-version.json

    install -d ${D}/${systemd_system_unitdir}
    install -m 0644 ${WORKDIR}/opentrons-system-server.service ${D}/${systemd_system_unitdir}/opentrons-system-server.service
}

FILES_${PN}_append = " ${systemd_system_unitdir/opentrons-system-server.service.d \
                       ${systemd_system_unitdir}/opentrons-system-server.service.d/system-server-version.conf \
                       "

RDEPENDS_${PN} += " python3-pyjwt nginx python-systemd"

inherit pipenv_app_bundle
