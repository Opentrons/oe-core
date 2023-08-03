SUMMARY = "Toradex WiFI configuration."
DESCRIPTION = "Allow for blacklisting of \
either the mwifiex or NXP proprietary driver \
based on the OVERRIDES settings."

inherit systemd

LICENSE = "CLOSED"

SRC_URI = " \
    file://${BPN}-mlan.conf \
    file://${BPN}-mwifiex.conf \
"

WIFI_CONFIG_FILE_SUFFIX="mwifiex"
WIFI_CONFIG_FILE_SUFFIX_default-nxp-proprietary-driver="mlan"

do_install () {
	install -d ${D}${sysconfdir}/modprobe.d/
	install -m 0644 ${WORKDIR}/${PN}-${WIFI_CONFIG_FILE_SUFFIX}.conf ${D}${sysconfdir}/modprobe.d/${PN}.conf
}

FILES_${PN} = "${sysconfdir}/modprobe.d/${PN}.conf"