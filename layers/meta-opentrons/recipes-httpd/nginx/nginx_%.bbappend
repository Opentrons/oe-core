FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI:append := " git://github.com/arut/nginx-rtmp-module.git;name=rtmp;destsuffix=nginx-rtmp-module;branch=master;protocol=https"
SRCREV_rtmp = "6c7719d0ba32e00b563ec70bd43dad11960fa9c4"

EXTRA_OECONF += "--add-module=${WORKDIR}/nginx-rtmp-module"

SRC_URI:append = " file://nginx-https.service file://nginx-http.service file://nginx-https.conf file://nginx-http.conf "

# note: not append, override to remove the basic service
SYSTEMD_SERVICE:${PN} = "nginx-http.service nginx-https.service"

do_install:append () {
    install -d ${D}${systemd_system_unitdir}
    install -d ${D}/var/www/localhost/html/stream/hls
    chown -R root:root ${D}/var/www/localhost/html/stream
    install -m 0644 ${WORKDIR}/nginx-http.service ${D}${systemd_system_unitdir}/nginx-http.service
    install -m 0644 ${WORKDIR}/nginx-https.service ${D}${systemd_system_unitdir}/nginx-https.service
    install -m 0644 ${WORKDIR}/nginx-http.conf ${D}${sysconfdir}/nginx/nginx-http.conf
    install -m 0644 ${WORKDIR}/nginx-https.conf ${D}${sysconfdir}/nginx/nginx-https.conf
    rm ${D}${systemd_system_unitdir}/nginx.service
    rm -rf ${D}${sysconfdir}/nginx/conf.d ${D}${sysconfdig}/nginx/server-conf.d
}

FILES:${PN}:append = " \
    ${systemd_system_unitdir}/nginx-http.service \
    ${systemd_system_unitdir}/nginx-https.service \
    ${sysconfdir}/nginx/nginx-http.conf \
    ${sysconfdir}/nginx/nginx-https.conf \
"
