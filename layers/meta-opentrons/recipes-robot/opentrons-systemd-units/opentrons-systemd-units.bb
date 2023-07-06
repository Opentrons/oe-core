SUMMARY = "Opentrons specific systemd units"
LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/Apache-2.0;md5=89aea4e17d99a7cacdbeed46a0096b10"

inherit systemd

FILESEXTRAPATHS_prepend = "${THISDIR}/files:"
SRC_URI += "\
      file://var-log-journal.service \
      file://opentrons-commit-machine-id.service \
      file://ot-commit-machine-id \
      "

SYSTEMD_AUTO_ENABLE = "enable"
SYSTEMD_SERVICE_${PN} += "var-log-journal.service"
SYSTEMD_SERVICE_${PN} += "opentrons-commit-machine-id.service"
SYSTEMD_PACKAGES = "${PN}"

FILES_${PN} += "\
      ${systemd_system_unitdir}/var-log-journal.service \
      ${systemd_system_unitdir}/opentrons-commit-machine-id.service \
      ${bindir}/ot-commit-machine-id \
      "

do_install() {
   install -d ${D}/${systemd_system_unitdir}
   install -m 0644 ${WORKDIR}/var-log-journal.service ${D}/${systemd_system_unitdir}/var-log-journal.service
   install -m 0644 ${WORKDIR}/opentrons-commit-machine-id.service ${D}/${systemd_system_unitdir}/opentrons-commit-machine-id.service

   # install supporting files
   install -d ${D}/${bindir}
   install -m 0644 ${WORKDIR}/ot-commit-machine-id ${D}/${bindir}/ot-commit-machine-id
}
