FILESEXTRAPATHS_append := ":${THISDIR}/${PN}"
SRC_URI += "\
      file://var-log-journal.service \
      "

SYSTEMD_SERVICE_${PN} += " var-log-journal.service"

FILES_${PN} += "\
      ${systemd_unitdir}/system/var-log-journal.service \
      "
