SUMMARY = "Kernel firmware for NXP Bluetooth chip"
LICENSE = "CLOSED"

FIRMWARE_BIN_interface-diversity-sd-sd = "sdsd8997_combo_v4.bin"
FIRMWARE_BIN_interface-diversity-sd-sd_mfg-mode = "sdio8997_sdio_combo.bin"
FIRMWARE_BIN_interface-diversity-sd-uart = "sdiouart8997_combo_v4.bin"
FIRMWARE_BIN_interface-diversity-sd-uart_mfg-mode = "sdio8997_uart_combo.bin"
FIRMWARE_BIN_interface-diversity-pcie-usb = "pcieusb8997_combo_v4.bin"
FIRMWARE_BIN_interface-diversity-pcie-usb_mfg-mode = "pcie8997_usb_combo.bin"

FILES_${PN} = "${base_libdir}/firmware/nxp"

# firmware binaries are generally machine specific
PACKAGE_ARCH = "${MACHINE_ARCH}"

do_install() {
    install -d ${D}${base_libdir}/firmware/nxp
    install -m 0644 ${S}/${FIRMWARE_BIN} ${D}${base_libdir}/firmware/nxp
}

COMPATIBLE_MACHINE = "(colibri-imx6ull|colibri-imx8x|verdin-imx8mm|verdin-imx8mp|apalis-imx8)"

addtask nxp_driver_unpack before do_patch after do_unpack
do_nxp_driver_unpack() {
    :
}

NXP_DRIVER_PACKAGE_interface-diversity-sd-uart="${NXP_PROPRIETARY_DRIVER_FILENAME};name=sd-uart-driver"
NXP_DRIVER_PACKAGE_interface-diversity-sd-uart_mfg-mode="${NXP_PROPRIETARY_MFG_TOOL_FILENAME};name=sd-uart-mfg-driver"
SRC_URI_append_interface-diversity-sd-uart = " ${NXP_PROPRIETARY_DRIVER_LOCATION}/${NXP_DRIVER_PACKAGE};subdir=archive.sd-uart "
SRC_URI[sd-uart-driver.sha256sum] = "${NXP_PROPRIETARY_DRIVER_SHA256}"
SRC_URI[sd-uart-mfg-driver.sha256sum] = "${NXP_PROPRIETARY_MFG_TOOL_SHA256}"
do_nxp_driver_unpack_interface-diversity-sd-uart() {
    DIRNAME=$(echo ${NXP_PROPRIETARY_DRIVER_FILENAME} | sed 's/\.zip//')
    DRVNAME=$(basename ${NXP_PROPRIETARY_DRIVER_FILENAME} | sed 's/zip/tar/')
    tar -C ${S} \
        --strip-components=1 \
        -xf ${WORKDIR}/archive.sd-uart/$DIRNAME/$DRVNAME \
        FwImage/${FIRMWARE_BIN}
}
do_nxp_driver_unpack_interface-diversity-sd-uart_mfg-mode() {
    DIRNAME=$(basename ${NXP_PROPRIETARY_MFG_TOOL_FILENAME} | sed 's/.zip//')
    install -m 0644 ${WORKDIR}/archive.sd-uart/$DIRNAME/FwImage/${FIRMWARE_BIN} ${S}/${FIRMWARE_BIN}
}


NXP_DRIVER_PACKAGE_interface-diversity-sd-sd="${NXP_PROPRIETARY_DRIVER_FILENAME};name=sd-sd-driver"
NXP_DRIVER_PACKAGE_interface-diversity-sd-sd_mfg-mode="${NXP_PROPRIETARY_MFG_TOOL_FILENAME};name=sd-sd-mfg-driver"
SRC_URI_append_interface-diversity-sd-sd = " ${NXP_PROPRIETARY_DRIVER_LOCATION}/${NXP_DRIVER_PACKAGE};subdir=archive.sd-sd "
SRC_URI[sd-sd-driver.sha256sum] = "${NXP_PROPRIETARY_DRIVER_SHA256}"
SRC_URI[sd-sd-mfg-driver.sha256sum] = "${NXP_PROPRIETARY_MFG_TOOL_SHA256}"
do_nxp_driver_unpack_interface-diversity-sd-sd() {
    DIRNAME=$(echo ${NXP_PROPRIETARY_DRIVER_FILENAME} | sed 's/\.zip//')
    DRVNAME=$(basename ${NXP_PROPRIETARY_DRIVER_FILENAME} | sed 's/zip/tar/')
    tar -C ${S} \
        --strip-components=1 \
        -xf ${WORKDIR}/archive.sd-sd/$DIRNAME/$DRVNAME \
        FwImage/${FIRMWARE_BIN}
}
do_nxp_driver_unpack_interface-diversity-sd-sd_mfg-mode() {
    DIRNAME=$(basename ${NXP_PROPRIETARY_MFG_TOOL_FILENAME} | sed 's/.zip//')
    install -m 0644 ${WORKDIR}/archive.sd-sd/$DIRNAME/FwImage/${FIRMWARE_BIN} ${S}/${FIRMWARE_BIN}
}

NXP_DRIVER_PACKAGE_interface-diversity-pcie-usb="${NXP_PROPRIETARY_DRIVER_FILENAME};name=pcie-usb-driver"
NXP_DRIVER_PACKAGE_interface-diversity-pcie-usb_mfg-mode="${NXP_PROPRIETARY_MFG_TOOL_FILENAME};name=pcie-usb-mfg-driver"
SRC_URI_append_interface-diversity-pcie-usb = " ${NXP_PROPRIETARY_DRIVER_LOCATION}/${NXP_DRIVER_PACKAGE};subdir=archive.pcie-usb "
SRC_URI[pcie-usb-driver.sha256sum] = "${NXP_PROPRIETARY_DRIVER_SHA256}"
SRC_URI[pcie-usb-mfg-driver.sha256sum] = "${NXP_PROPRIETARY_MFG_TOOL_SHA256}"
do_nxp_driver_unpack_interface-diversity-pcie-usb() {
    DRVNAME=$(basename ${NXP_PROPRIETARY_DRIVER_FILENAME} | sed 's/zip/tar/')
    tar -C ${S} \
        --strip-components=1 \
        -xf ${WORKDIR}/archive.pcie-usb/$DRVNAME \
        FwImage/${FIRMWARE_BIN}
}
do_nxp_driver_unpack_interface-diversity-pcie-usb_mfg-mode() {
    DIRNAME=$(basename ${NXP_PROPRIETARY_MFG_TOOL_FILENAME} | sed 's/.zip//')
    install -m 0644 ${WORKDIR}/archive.pcie-usb/$DIRNAME/FwImage/${FIRMWARE_BIN} ${S}/${FIRMWARE_BIN}
}

