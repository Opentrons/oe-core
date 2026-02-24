FILESEXTRAPATHS:prepend := "${THISDIR}/${PN}:"
SRC_URI += " \
        file://touchscreen-goodix.cfg \
        file://SPI-CAN.cfg \
        file://hidraw.cfg \
        file://0001-pmdomain-imx-scu-pd-clear-is_off.patch \
        file://devicemapper.cfg \
        file://disable-vt-sysrq.cfg \
        "
