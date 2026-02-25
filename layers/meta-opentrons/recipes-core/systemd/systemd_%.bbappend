# xz support was present on older builds and must be present now to read journal files
# from before the upgrade
PACKAGECONFIG:append = " xz "
FILESEXTRAPATHS:append := "${THISDIR}/files:"

SRC_URI += " \
    file://dns-fallback.conf \
    file://ntp-fallback.conf \
    file://no_vt.conf \
"

do_install:append() {
    # Google DNS + NTP servers are not reachable from China so use alternatives.
    install -d 0755 ${D}${sysconfdir}/systemd/resolved.conf.d
    install -d 0755 ${D}${sysconfdir}/systemd/timesyncd.conf.d
    install -m 0644 ${WORKDIR}/dns-fallback.conf ${D}${sysconfdir}/systemd/resolved.conf.d/
    install -m 0644 ${WORKDIR}/ntp-fallback.conf ${D}${sysconfdir}/systemd/timesyncd.conf.d/

    # Disable AutoVT spawning (logind)
    install -d 0755 ${D}${sysconfdir}/systemd/logind.conf.d
    install -m 0644 ${WORKDIR}/no_vt.conf \
        ${D}${sysconfdir}/systemd/logind.conf.d/no-vt.conf

    # Mask getty on tty1
    install -d ${D}${sysconfdir}/systemd/system
    ln -sf /dev/null ${D}${sysconfdir}/systemd/system/getty@tty1.service
}

FILES:${PN} += " \
    ${sysconfdir}/systemd/resolved.conf.d \
    ${sysconfdir}/systemd/timesyncd.conf.d \
    ${sysconfdir}/systemd/logind.conf.d \
    ${sysconfdir}/systemd/resolved.conf.d/dns-fallback.conf \
    ${sysconfdir}/systemd/timesyncd.conf.d/ntp-fallback.conf \
    ${sysconfdir}/systemd/logind.conf.d/no-vt.conf \
    ${sysconfdir}/systemd/system/getty@tty1.service \
"
