SUMMARY = "Opentrons specific systemd units"
LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/Apache-2.0;md5=89aea4e17d99a7cacdbeed46a0096b10"

inherit systemd

FILESEXTRAPATHS_append := ":${THISDIR}/${PN}"
SRC_URI += "\
      file://var-log-journal.service \
      "

SYSTEMD_SERVICE_${PN} += " var-log-journal.service"
SYSTEMD_PACKAGES = "${PN}"

FILES_${PN} += "\
      ${systemd_system_unitdir}/system/var-log-journal.service \
      "

do_install() {
   install -d ${D}${systemd_system_unitdir}
   install -m 0644 ${WORKDIR}/var-log-journal.service ${D}${systemd_system_unitdir}/var-log-journal.service
}
