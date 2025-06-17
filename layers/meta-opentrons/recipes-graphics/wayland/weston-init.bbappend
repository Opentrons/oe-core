FILESEXTRAPATHS:prepend := "${THISDIR}/files:"
SRC_URI += " file://manage-users.conf file://galcore.conf "

do_install:append() {
    install -D -p -m0644 ${WORKDIR}/manage-users.conf ${D}${systemd_system_unitdir}/weston.service.d/manage-users.conf
    install -D -p -m0644 ${WORKDIR}/galcore.conf ${D}/etc/modprobe.d/galcore.conf
}

FILES:${PN} += " \
    ${systemd_system_unitdir}/weston.service.d ${systemd_system_unitdir}/weston.service.d/manage-users.conf \
    /etc/modprobe.d /etc/modprobe.d/galcore.conf \
"
