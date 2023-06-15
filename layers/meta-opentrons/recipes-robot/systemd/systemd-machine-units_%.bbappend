FILESEXTRAPATHS_append := ":${THISDIR}/${PN}"
SRC_URI += "\
      file://var-log-journal.service \
      "

SYSTEMD_SERVICE_${PN} += " var-log-journal.service"
SYSTEMD_PACKAGES = "${PN}"

FILES_${PN} += "\
      ${systemd_unitdir}/system/var-log-journal.service \
      "

do_install_append() {
   install -d ${D}${systemd_unitdir}/system/
   install -m 0644 ${WORKDIR}/var-log-journal.service ${D}${systemd_unitdir}/system
}
