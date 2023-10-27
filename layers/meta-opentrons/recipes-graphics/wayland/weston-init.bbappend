FILESEXTRAPATHS:prepend := "${THISDIR}/files:"
SRC_URI += " file://manage-users.conf file://create-home-weston.service"

do_install:append() {
    install -D -p -m0644 ${WORKDIR}/manage-users.conf ${D}${systemd_system_unitdir}/weston.service.d/manage-users.conf
    install -D -p -m0644 ${WORKDIR}/create-home-weston.service ${D}${systemd_system_unitdir}/create-home-weston.service
}

FILES:${PN} += " ${systemd_system_unitdir}/weston.service.d ${systemd_system_unitdir}/weston.service.d/manage-users.conf ${systemd_system_unitdir}/create-home-weston.service"
