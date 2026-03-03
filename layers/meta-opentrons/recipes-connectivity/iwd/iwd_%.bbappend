FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI += "\
           file://main.conf \
           "

FILES:${PN} += " \
   /etc/iwd \
   /etc/iwd/main.conf \
"

do_install:append() {
   install -d ${D}/etc/iwd
   install -m 0644 ${WORKDIR}/main.conf ${D}/etc/iwd/main.conf
}
