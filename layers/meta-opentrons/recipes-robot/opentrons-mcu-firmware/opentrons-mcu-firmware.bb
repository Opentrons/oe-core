DESCRIPTION = "builds and installs the update binaries for the ot3 firmware."
LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://LICENSE;md5=86d3f3a95c324c9479bd8986968f4327"

inherit externalsrc pkgconfig cmake

FIRMWARE_DIR="${libdir}/firmware"

EXTERNALSRC = "${@os.path.abspath(os.path.join("${TOPDIR}", os.pardir, os.pardir, "ot3-firmware"))}"
DEPENDS += " cmake-native"

do_configure(){
    cd ${S}/
    bbnote "cmake --preset=cross-no-directory-reqs -B ${B}/build-cross --install-prefix=${B}/dist ."
    cmake --preset=cross-no-directory-reqs -B ${B}/build-cross --install-prefix=${B}/dist .

}

do_compile(){
    cd ${S}/

    cmake --build ${B}/build-cross --target firmware-applications
    cmake --install ${B}/build-cross --component Applications
}

do_install(){
    # install the compiled binaries to /usr/lib/firmware
    install -d ${D}${FIRMWARE_DIR}
    find ${B}/dist/applications -type f -exec install -m 0644 {} ${D}${FIRMWARE_DIR} \;
}

# since we are compiling binaries for the subsystem which has a different arch to linux we need
# to ignore the architecture.
INSANE_SKIP_${PN} += "arch"

FILES_${PN} += "${libdir}/firmware/head-*.hex \
                ${libdir}/firmware/gantry-*.hex \
                ${libdir}/firmware/gripper-*.hex \
                ${libdir}/firmware/pipettes-*.hex \
                ${libdir}/firmware/rear-panel-*.bin \
                ${libdir}/firmware/opentrons-firmware.json"
