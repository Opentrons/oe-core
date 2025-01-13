SUMMARY = "Opentrons specific systemd units"
LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/Apache-2.0;md5=89aea4e17d99a7cacdbeed46a0096b10"

inherit systemd

FILESEXTRAPATHS:prepend = "${THISDIR}/files:"
SRC_URI += "\
      file://var-log-journal.service \
      file://opentrons-commit-machine-id.service \
      file://opentrons-clear-fontconfig-cache.service \
      file://ot-commit-machine-id \
      file://ot-clear-fontconfig-cache \
      "

SYSTEMD_AUTO_ENABLE = "enable"
SYSTEMD_SERVICE:${PN} += "var-log-journal.service"
SYSTEMD_SERVICE:${PN} += "opentrons-commit-machine-id.service"
SYSTEMD_SERVICE:${PN} += "opentrons-clear-fontconfig-cache.service"
SYSTEMD_PACKAGES = "${PN}"

FILES:${PN} += "\
      ${systemd_system_unitdir}/var-log-journal.service \
      ${systemd_system_unitdir}/opentrons-commit-machine-id.service \
      ${systemd_system_unitdir}/opentrons-clear-fontconfig-cache.service \
      ${bindir}/ot-commit-machine-id \
      ${bindir}/ot-clear-fontconfig-cache \
      "

do_install() {
   install -d ${D}/${systemd_system_unitdir}
   install -m 0644 ${WORKDIR}/var-log-journal.service ${D}/${systemd_system_unitdir}/
   install -m 0644 ${WORKDIR}/opentrons-commit-machine-id.service ${D}/${systemd_system_unitdir}/
   install -m 0644 ${WORKDIR}/opentrons-clear-fontconfig-cache.service ${D}/${systemd_system_unitdir}/

   # install supporting files
   install -d ${D}/${bindir}
   install -m 0744 ${WORKDIR}/ot-commit-machine-id ${D}/${bindir}/
   install -m 0744 ${WORKDIR}/ot-clear-fontconfig-cache ${D}/${bindir}/
}
