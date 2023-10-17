FILESEXTRAPATHS:append := ":${THISDIR}/${PN}"

SRC_URI:append := " \
    file://opentrons-journald.conf \
"

do_install:append() {
   install -D -m0644 ${WORKDIR}/opentrons-journald.conf ${D}${systemd_unitdir}/journald.conf.d/01-storage.conf
}
