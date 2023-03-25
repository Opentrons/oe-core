FILESEXTRAPATHS_prepend := "${THISDIR}/${PN}:"
SRC_URI += " \
        file://touchscreen-goodix.cfg \
        file://SPI-CAN.cfg \
        file://custom-logo.cfg \
        file://logo_custom_clut224.ppm \
        "

do_unpack_append(){
    bb.build.exec_func('add_logo', d)
}

add_logo(){
   cp ${WORKDIR}/logo_custom_clut224.ppm ${S}/drivers/video/logo/logo_custom_clut224.ppm
}
