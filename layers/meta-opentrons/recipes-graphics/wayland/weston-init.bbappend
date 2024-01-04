FILESEXTRAPATHS:prepend := "${THISDIR}/files:"
SRC_URI += " file://manage-users.conf"

do_install:append() {
    install -D -p -m0644 ${WORKDIR}/manage-users.conf ${D}${systemd_system_unitdir}/weston.service.d/manage-users.conf
}

FILES:${PN} += " ${systemd_system_unitdir}/weston.service.d ${systemd_system_unitdir}/weston.service.d/manage-users.conf"
