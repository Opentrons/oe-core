inherit systemd
FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI += "file://system-connections-location.conf \
            file://disable-uap0.conf \
            file://wired-linklocal.nmconnection \
            file://wired.nmconnection \
            file://opentrons-init-systemconnections.service\
"

FILES:${PN} += "/etc/NetworkManager/conf.d/system-connections-location.conf \
                /etc/NetworkManager/conf.d/disable-uap0.conf \
                ${systemd_system_unitdir}/opentrons-init-systemconnections.service \
                /usr/share/default-connections/wired-linklocal.nmconnection \
                /usr/share/default-connections/wired.nmconnection \
"

do_install:append() {
	install -d ${D}/var/lib/NetworkManager/system-connections
	install -d ${D}/etc/NetworkManager/conf.d
	install -m 644 ${WORKDIR}/system-connections-location.conf ${D}/etc/NetworkManager/conf.d/
	install -m 644 ${WORKDIR}/disable-uap0.conf ${D}/etc/NetworkManager/conf.d/
    install -d ${D}/usr/share/default-connections
    install -m 600 ${WORKDIR}/wired-linklocal.nmconnection ${D}/usr/share/default-connections/wired-linklocal.nmconnection
    install -m 600 ${WORKDIR}/wired.nmconnection ${D}/usr/share/default-connections/wired.nmconnection
    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${WORKDIR}/opentrons-init-systemconnections.service ${D}${systemd_system_unitdir}/opentrons-init-systemconnections.service

}

SYSTEMD_AUTO_ENABLE = "enable"
SYSTEMD_SERVICE:${PN}:append = " opentrons-init-systemconnections.service"
