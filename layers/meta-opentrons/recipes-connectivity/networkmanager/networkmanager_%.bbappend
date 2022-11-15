FILESEXTRAPATHS_prepend := "${THISDIR}/files:"

SRC_URI += "file://system-connections-location.conf"

FILES_${PN} += "/etc/NetworkManager/conf.d/system-connections-location.conf"

do_install_append() {
	install -d ${D}/var/lib/NetworkManager/system-connections
	install -d ${D}/etc/NetworkManager/conf.d
	install -m 644 ${WORKDIR}/system-connections-location.conf ${D}/etc/NetworkManager/conf.d/
}
