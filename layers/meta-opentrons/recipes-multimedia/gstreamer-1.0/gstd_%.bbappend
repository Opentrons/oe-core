inherit systemd

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI += "\
          file://gstd.service \
          "

FILES:${PN}:append := " ${systemd_unitdir}/gstd.service "

SYSTEMD_PACKAGES = "${PN}"
SYSTEMD_SERVICE:${PN} = "gstd.service"

do_install:append () {
    install -d ${D}${systemd_unitdir}/system/
    install -m 0644 ${WORKDIR}/gstd.service ${D}${systemd_unitdir}/system
}
