FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI += " \
    file://plymouthd.defaults \
    file://logo-splash.png \
    file://logo-splash.plymouth \
    file://logo-splash.script \
"

do_install:append () {
    install -m 0644 ${WORKDIR}/plymouthd.defaults ${D}${datadir}/plymouth/plymouthd.defaults
    install -d 0644 ${D}${datadir}/plymouth/themes/logo-splash
    install -m 0644 ${WORKDIR}/logo-splash.png ${D}${datadir}/plymouth/themes/logo-splash/logo-splash.png
    install -m 0644 ${WORKDIR}/logo-splash.plymouth ${D}${datadir}/plymouth/themes/logo-splash/logo-splash.plymouth
    install -m 0644 ${WORKDIR}/logo-splash.script ${D}${datadir}/plymouth/themes/logo-splash/logo-splash.script
}

FILES:${PN}:append := "\
    ${datadir}/plymouth/themes/logo-splash \
    ${datadir}/plymouth/themes/logo-splash/logo-splash.png \
    ${datadir}/plymouth/themes/logo-splash/logo-splash.script \
    ${datadir}/plymouth/plymouthd.defaults \
"
