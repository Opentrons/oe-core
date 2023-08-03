SUMMARY = "Kernel loadable module for NXP Bluetooth chip"
LICENSE = "CLOSED"

inherit module

DEPENDS="kernel-module-wifimrvl"
RPROVIDES_${PN}_append_interface-diversity-sd-sd = " kernel-module-bt8xxx "
RPROVIDES_${PN}_append_interface-diversity-pcie-usb = " kernel-module-bt8xxx "

KERNEL_MODULE_PROBECONF_append_interface-diversity-sd-sd = " bt8xxx "
module_conf_bt8xxx_interface-diversity-sd-sd_mfg-mode = "options bt8xxx fw_name=nxp/sdio8997_sdio_combo.bin"

KERNEL_MODULE_PROBECONF_append_interface-diversity-pcie-usb = " bt8xxx "
module_conf_bt8xxx_interface-diversity-pcie-usb_mfg-mode = "options bt8xxx fw_name=nxp/pcie8997_usb_combo.bin"

SRC_URI = " \
    file://0001-makefile.patch \
"

S = "${WORKDIR}/mbt_src"

RDEPENDS_${PN} += "toradex-wifi-config"

COMPATIBLE_MACHINE = "(colibri-imx6ull|colibri-imx8x|verdin-imx8mm|verdin-imx8mp|apalis-imx8)"

addtask nxp_driver_unpack before do_patch after do_unpack
do_nxp_driver_unpack() {
    :
}

SRC_URI_append_interface-diversity-sd-sd = " ${NXP_PROPRIETARY_DRIVER_LOCATION}/${NXP_PROPRIETARY_DRIVER_FILENAME};name=sd-sd-driver;subdir=archive.sd-sd "
SRC_URI[sd-sd-driver.sha256sum] = "${NXP_PROPRIETARY_DRIVER_SHA256}"

do_nxp_driver_unpack_interface-diversity-sd-sd() {
    DRVNAME=$(basename ${NXP_PROPRIETARY_DRIVER_FILENAME} | sed 's/zip/tar/')
    DIRNAME=$(echo ${NXP_PROPRIETARY_DRIVER_FILENAME} | sed 's/\.zip//')
    tar -C ${WORKDIR}/archive.sd-sd/ -xf ${WORKDIR}/archive.sd-sd/$DIRNAME/$DRVNAME
    for i in `ls ${WORKDIR}/archive.sd-sd/*-src.tgz`; do
        tar --strip-components=1 -C ${WORKDIR} \
            -xf $i
    done
}

SRC_URI_append_interface-diversity-pcie-usb = " ${NXP_PROPRIETARY_DRIVER_LOCATION}/${NXP_PROPRIETARY_DRIVER_FILENAME};name=pcie-usb-driver;subdir=archive.pcie-usb "
SRC_URI[pcie-usb-driver.sha256sum] = "${NXP_PROPRIETARY_DRIVER_SHA256}"
do_nxp_driver_unpack_interface-diversity-pcie-usb() {
    DRVNAME=$(basename ${NXP_PROPRIETARY_DRIVER_FILENAME} | sed 's/zip/tar/')
    tar -C ${WORKDIR}/archive.pcie-usb/ -xf ${WORKDIR}/archive.pcie-usb/$DRVNAME
    for i in `ls ${WORKDIR}/archive.pcie-usb/*-src.tgz`; do
        tar --strip-components=1 -C ${WORKDIR} \
            -xf $i
    done
}

