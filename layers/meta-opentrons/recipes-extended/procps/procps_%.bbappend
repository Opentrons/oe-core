FILESEXTRAPATHS:append := "${THISDIR}/files"

SRC_URI += "file://95-opentrons.conf"

do_install:append() {
	install -d ${D}${sysconfdir}/sysctl.d
	install -m 0644 ${WORKDIR}/95-opentrons.conf ${D}${sysconfdir}/sysctl.d/95-opentrons.conf
}
