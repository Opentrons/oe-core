DESCRIPTION = "Disable Ctrl+Alt+F1..F8 (except F7) VT switching shortcuts"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = " \
    file://opentrons-no-vt.map \
    file://vconsole.conf \
"

S = "${WORKDIR}"

RDEPENDS:${PN} += "kbd"

# Provide keymap + vconsole default so systemd-vconsole-setup loads it at boot.
do_install() {
    install -d ${D}${datadir}/kbd/keymaps
    install -m 0644 ${WORKDIR}/opentrons-no-vt.map ${D}${datadir}/kbd/keymaps/opentrons-no-vt.map

    install -d ${D}${sysconfdir}
    install -m 0644 ${WORKDIR}/vconsole.conf ${D}${sysconfdir}/vconsole.conf
}

FILES:${PN} += " \
    ${datadir}/kbd/keymaps/opentrons-no-vt.map \
    ${sysconfdir}/vconsole.conf \
"
