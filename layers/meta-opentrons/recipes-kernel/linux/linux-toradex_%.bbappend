FILESEXTRAPATHS:prepend := "${THISDIR}/${PN}:"
SRC_URI += " \
        file://touchscreen-goodix.cfg \
        file://SPI-CAN.cfg \
        "
