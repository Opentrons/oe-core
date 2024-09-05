# xz support was present on older builds and must be present now to read journal files
# from before the upgrade
PACKAGECONFIG:append = " xz "
FILESEXTRAPATHS:append := "${THISDIR}/files:"

SRC_URI += " \
    file://dns-fallback.conf \
    file://ntp-fallback.conf \
"

do_install:append() {
    # Google DNS + NTP servers are not reachable from China so use alternatives.
    install -d 0755 ${D}${sysconfdir}/systemd/resolved.conf.d
    install -d 0755 ${D}${sysconfdir}/systemd/timesyncd.conf.d
    install -m 0644 ${WORKDIR}/dns-fallback.conf ${D}${sysconfdir}/systemd/resolved.conf.d/
    install -m 0644 ${WORKDIR}/ntp-fallback.conf ${D}${sysconfdir}/systemd/timesyncd.conf.d/
}

FILES:${PN} += " \
    ${sysconfdir}/systemd/resolved.conf.d \
    ${sysconfdir}/systemd/timesyncd.conf.d \
    ${sysconfdir}/systemd/resolved.conf.d/ntp-fallback.conf \
    ${sysconfdir}/systemd/timesyncd.conf.d/dns-fallback.conf \
"
