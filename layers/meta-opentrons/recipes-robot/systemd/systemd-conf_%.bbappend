
SRC_URI_append = "\
    file://opentrons-journald.conf
"

do_install_append() {
   install -D -m0644 ${WORKDIR}/opentrons-journald.conf ${D}${systemd_unitdir}/journald.conf.d/01-storage.conf
}
