FILESEXTRAPATHS_prepend := "${THISDIR}/files:"

SRC_URI += "file://pip.conf"

do_install_append() {
    install -d ${D}/etc/pip
    install -m 644 ${WORKDIR}/pip.conf ${D}/etc/pip/pip.conf
}
