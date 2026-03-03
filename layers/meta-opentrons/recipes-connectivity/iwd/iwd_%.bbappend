
FILES:${PN} += " \
   /etc/iwd \
"

do_install:append() {
   install -m0666 -d ${D}/etc/iwd
}
