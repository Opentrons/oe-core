DESCRIPTION = "Installs Byonoy Python library and firmware files."
LICENSE = "CLOSED"

FILESEXTRAPATHS:prepend = "${THISDIR}/files:"
WHLNAME = "byonoy_devices-${PV}-cp312-cp312-linux_aarch64.whl" 
SRC_URI += "https://git.byonoy.com/public/-/packages/pypi/byonoy-devices/${PV}/files/2294;downloadfilename=${WHLNAME};name=libbyonoy \
            file://absorbance-96@v8.byoup \
"
SRC_URI[libbyonoy.sha256sum] = "72813902c34505b188ecc3f891f198a0d03b508d356df3c19c08afd8bf33c2bb"
FIRMWARE_DIR="${libdir}/firmware"

COMPATIBLE_HOST = "aarch64.*-linux"

# The following is needed for unversioned pre-built libraries
# See https://docs.yoctoproject.org/dev-manual/prebuilt-libraries.html#non-versioned-libraries
INSANE_SKIP:${PN} = "ldflags already-stripped"
INHIBIT_PACKAGE_STRIP = "1"
INHIBIT_SYSROOT_STRIP = "1"
SOLIBS = ".so"
FILES_SOLIBSDEV = ""

do_install:append() {
        # install the python library to the robot server directory
        install -d ${D}/opt/opentrons-robot-server/
        ${PYTHON} -m pip install \
           -t ${D}/opt/opentrons-robot-server \
           --no-compile \
           --force-reinstall \
           --no-deps \
           --no-build-isolation \
           --progress-bar off \
           --platform=linux_aarch64 \
           ${WORKDIR}/${WHLNAME}
           
        # install the firmware files to /usr/lib/firmware
        install -d ${D}${FIRMWARE_DIR}
        install -m 644 ${WORKDIR}/absorbance-96@v8.byoup ${D}${FIRMWARE_DIR}/
}

RDEPENDS:${PN} += "hidapi libudev "

FILES:${PN} += "${libdir}/firmware/absorbance-96@v8.byoup \
                /opt/opentrons-robot-server/byonoy_devices \
                /opt/opentrons-robot-server/byonoy_devices-${PV}.dist-info \
"
