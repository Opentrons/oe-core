FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI += "https://github.com/arut/nginx-rtmp-module.git;name=rtmp;destsuffix=nginx-rtmp-module;branch=master"
SRCREV_rtmp = "6c7719d0ba32e00b563ec70bd43dad11960fa9c4"

EXTRA_OECONF += "--add-module=${WORKDIR}/nginx-rtmp-module"

SRC_URI:append = " file://nginx.service "

do_install:append () {
    install -d ${D}${systemd_system_unitdir}
    install -d ${D}/var/www/localhost/html/stream/hls
    chown -R root:root ${D}/var/www/localhost/html/stream
    install -m 0644 ${WORKDIR}/nginx.service ${D}${systemd_system_unitdir}/nginx.service
}

FILES:${PN}:append = " ${systemd_system_unitdir/nginx.service \ "
