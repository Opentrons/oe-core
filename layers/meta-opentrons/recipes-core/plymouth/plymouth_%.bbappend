FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI += " \
    file://logo-splash.png \
    file://logo-splash.plymouth \
"

PACKAGECONFIG = "pango drm"

EXTRA_OECONF += "--with-udev --with-runtimedir=/run"

do_install:append () {
    install -d 0644 ${D}${datadir}/plymouth/themes/logo-splash
    install -m 0644 ${WORKDIR}/logo-splash.png ${D}${datadir}/plymouth/themes/logo-splash/logo-splash.png
    install -m 0644 ${WORKDIR}/logo-splash.plymouth ${D}${datadir}/plymouth/themes/logo-splash/logo-splash.plymouth
}

FILES:${PN}:append := "\
    ${datadir}/plymouth/themes/logo-splash \
    ${datadir}/plymouth/themes/logo-splash/logo-splash.png \
    ${datadir}/plymouth/themes/logo-splash/logo-splash.script \
"
