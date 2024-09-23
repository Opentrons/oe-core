DESCRIPTION = "Installs Byonoy Python library and firmware files."
LICENSE = "CLOSED"

FILESEXTRAPATHS:prepend = "${THISDIR}/files:"
SRC_URI += "file://byonoy_devices.so \
            file://libbyonoy_device_library.so \
            file://absorbance-96@v1.0.2.byoup \
"
FIRMWARE_DIR="${libdir}/firmware"

inherit python_setuptools_build_meta

COMPATIBLE_HOST = "aarch64.*-linux"

# The following is needed for unversioned pre-built libraries
# See https://docs.yoctoproject.org/dev-manual/prebuilt-libraries.html#non-versioned-libraries
INSANE_SKIP:${PN} = "ldflags already-stripped"
INHIBIT_PACKAGE_STRIP = "1"
INHIBIT_SYSROOT_STRIP = "1"
SOLIBS = ".so"
FILES_SOLIBSDEV = ""

do_install:append() {
        # install the python library to /usr/lib/python3.10/site-packages
        install -m 755 ${WORKDIR}/byonoy_devices.so ${D}${libdir}/python3.10/site-packages
        install -m 755 ${WORKDIR}/libbyonoy_device_library.so ${D}${libdir}
        # install the firmware files to /usr/lib/firmware
        install -d ${D}${FIRMWARE_DIR}
        install -m 644 ${WORKDIR}/absorbance-96@v1.0.2.byoup ${D}${FIRMWARE_DIR}
}

RDEPENDS:${PN} += "hidapi"

FILES:${PN} += "${libdir}/libbyony_device_library.so \
                ${libdir}/firmware/absorbance-96@v1.0.2.byoup \
                ${libdir}/python3.10/site-packages/byonoy_devices.so \
"
