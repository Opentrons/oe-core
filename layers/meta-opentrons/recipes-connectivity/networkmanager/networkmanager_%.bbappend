FILESEXTRAPATHS_prepend := "${THISDIR}/files:"

SRC_URI += "file://NetworkManager-extra.conf"

FILES_${PN} += "/etc/NetworkManager/conf.d/NetworkManager-extra.conf"

do_install_append() {
	install -d ${D}/etc/NetworkManager/conf.d
	install -m 644 ${WORKDIR}/NetworkManager-extra.conf ${D}/etc/NetworkManager/conf.d/NetworkManager-extra.conf
}
