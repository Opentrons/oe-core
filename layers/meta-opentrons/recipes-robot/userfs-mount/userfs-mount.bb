LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM ?= "file://${COMMON_LICENSE_DIR}/Apache-2.0;md5=89aea4e17d99a7cacdbeed46a0096b10"
SUMMARY = "ot3 userfs partition mount units"
inherit systemd
SYSTEMD_AUTO_ENABLE = "enable"
SYSTEMD_SERVICE_${PN} = " userfs.mount home.mount var.mount data.mount"

SRC_URI_append = " file://userfs.mount \
        file://home.mount \
	file://var.mount \
	file://data.mount \
"
FILES_${PN} += "${systemd_unitdir}/system/userfs.mount \
        ${systemd_unitdir}/system/home.mount \
        ${systemd_unitdir}/system/var.mount \
        ${systemd_unitdir}/system/data.mount \
"

do_install_append() {
  install -d ${D}/${systemd_unitdir}/system
  install -m 0644 ${WORKDIR}/userfs.mount ${D}/${systemd_unitdir}/system
  install -m 0644 ${WORKDIR}/home.mount ${D}/${systemd_unitdir}/system
  install -m 0644 ${WORKDIR}/var.mount ${D}/${systemd_unitdir}/system
  install -m 0644 ${WORKDIR}/data.mount ${D}/${systemd_unitdir}/system
}
