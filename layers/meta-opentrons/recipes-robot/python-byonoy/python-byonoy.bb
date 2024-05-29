DESCRIPTION = "installs Byonoy Python library and firmware files"
LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://LICENSE;md5=3b83ef96387f14655fc854ddc3c6bd57"

FILESEXTRAPATHS:prepend = "${THISDIR}/files:"
SRC_URI += "file://pybyonoy_device_library.cpython-310-aarch64-linux-gnu.so \
            file://libbyonoy_device_library.so \
            file://absorbance-96-v1.0.0.byoup \
            file://absorbance-96-v1.0.2.byoup \
"
FIRMWARE_DIR="${libdir}/firmware"

inherit python_setuptools_build_meta

do_install:append() {
        # install the python library to /usr/lib/python3.10/site-packages
        install -m 644 ${WORKDIR}/pybyonoy_device_library.cpython-310-aarch64-linux-gnu.so ${D}${libdir}/python3.10/site-packages
        install -m 644 ${WORKDIR}/libbyonoy_device_library.so ${D}${libdir}
        # install the firmware files to /usr/lib/firmware
        install -d ${D}${FIRMWARE_DIR}
        install -m 644 ${WORKDIR}/absorbance-96-v1.0.0.byoup ${D}${FIRMWARE_DIR}
        install -m 644 ${WORKDIR}/absorbance-96-v1.0.2.byoup ${D}${FIRMWARE_DIR}
}

FILES:${PN} += "${libdir}/libbyony_device_library.so \
                ${libdir}/firmware/Absorbance_96_Auto_2024-05-03-V1.0.0.byoup \
                ${libdir}/firmware/Absorbance_96_Auto_2024-05-14-V1.0.2.byoup \
                ${libdir}/python3.10/site-packages/pybyonoy_device_library.cpython-310-aarch64-linux-gnu.so \
"
