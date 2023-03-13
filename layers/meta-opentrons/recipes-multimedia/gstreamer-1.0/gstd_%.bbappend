inherit systemd

FILESEXTRAPATHS_prepend := "${THISDIR}/files:"

SRC_URI += "\
          file://gstd.service \
          "

FILES_${PN}_append := " ${systemd_unitdir}/gstd.service "

SYSTEMD_PACKAGES = "${PN}"
SYSTEMD_SERVICE_${PN} = "gstd.service"

do_install_append () {
    install -d ${D}${systemd_unitdir}/system/
    install -m 0644 ${WORKDIR}/gstd.service ${D}${systemd_unitdir}/system
}
