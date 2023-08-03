SUMMARY = "Kernel loadable module for NXP WiFi chip"
LICENSE = "CLOSED"

inherit module

RPROVIDES_${PN}_append_interface-diversity-sd-uart= " kernel-module-moal kernel-module-mlan "
RPROVIDES_${PN}_append_interface-diversity-sd-sd = " kernel-module-sd8xxx kernel-module-mlan "
RPROVIDES_${PN}_append_interface-diversity-pcie-usb = " kernel-module-pcie8xxx kernel-module-mlan "

KERNEL_MODULE_WIFI_INTERFACE_interface-diversity-sd-uart = "moal"
KERNEL_MODULE_WIFI_INTERFACE_interface-diversity-sd-sd = "sd8xxx"
KERNEL_MODULE_WIFI_INTERFACE_interface-diversity-pcie-usb = "pcie8xxx"

KERNEL_MODULE_PROBECONF_append_interface-diversity-sd-uart= " ${KERNEL_MODULE_WIFI_INTERFACE} "
module_conf_moal_interface-diversity-sd-uart= "options moal cal_data_cfg=nxp/cal_data.conf cfg80211_wext=12"
module_conf_moal_interface-diversity-sd-uart_mfg-mode = "options moal cal_data_cfg=none cfg80211_wext=0xf mfg_mode=1 fw_name=nxp/sdio8997_uart_combo.bin"

KERNEL_MODULE_PROBECONF_append_interface-diversity-sd-sd = " ${KERNEL_MODULE_WIFI_INTERFACE} "
module_conf_sd8xxx_interface-diversity-sd-sd = "options sd8xxx cal_data_cfg=nxp/cal_data.conf cfg80211_wext=12"
module_conf_sd8xxx_interface-diversity-sd-sd_mfg-mode = "options sd8xxx cal_data_cfg=none cfg80211_wext=0xf mfg_mode=1 fw_name=nxp/sdio8997_sdio_combo.bin"

KERNEL_MODULE_PROBECONF_append_interface-diversity-pcie-usb = " ${KERNEL_MODULE_WIFI_INTERFACE} "
module_conf_pcie8xxx_interface-diversity-pcie-usb = "options pcie8xxx cal_data_cfg=nxp/cal_data.conf cfg80211_wext=12"
module_conf_pcie8xxx_interface-diversity-pcie-usb_mfg-mode = "options pcie8xxx cal_data_cfg=none cfg80211_wext=0xf mfg_mode=1 fw_name=nxp/pcie8997_usb_combo.bin"

SRC_URI = "\
    file://cal_data.conf \
    file://0001-makefile.patch \
"

SRC_URI_interface-diversity-sd-uart = "\
    file://cal_data.conf \
    file://0001-Replace-KERNELDIR-for-KERNEL_SRC.patch\
    file://0002-add-install-target.patch\
"

S = "${WORKDIR}/wlan_src"

DEPENDS += "bc-native"
RDEPENDS_${PN} += "toradex-wifi-config"

do_install_append() {
    install -d ${D}${base_libdir}/firmware/nxp
    install -m 0644 ${WORKDIR}/cal_data.conf ${D}${base_libdir}/firmware/nxp
}

FILES_${PN} += "${base_libdir}/firmware/nxp"

COMPATIBLE_MACHINE = "(colibri-imx6ull|colibri-imx8x|verdin-imx8mm|verdin-imx8mp|apalis-imx8)"

addtask nxp_driver_unpack before do_patch after do_unpack
do_nxp_driver_unpack() {
    :
}

SRC_URI_append_interface-diversity-sd-uart = " ${NXP_PROPRIETARY_DRIVER_LOCATION}/${NXP_PROPRIETARY_DRIVER_FILENAME};name=sd-uart-driver;subdir=archive.sd-uart "
SRC_URI[sd-uart-driver.sha256sum] = "${NXP_PROPRIETARY_DRIVER_SHA1}"
do_nxp_driver_unpack_interface-diversity-sd-uart() {
    DIRNAME=$(echo ${NXP_PROPRIETARY_DRIVER_FILENAME} | sed 's/\.zip//')
    DRVNAME=$(basename ${NXP_PROPRIETARY_DRIVER_FILENAME} | sed 's/zip/tar/')
    tar --strip-components=1 -C ${WORKDIR}/archive.sd-uart/ -xf ${WORKDIR}/archive.sd-uart/$DIRNAME/$DRVNAME
    for i in `ls ${WORKDIR}/archive.sd-uart/*-src.tgz`; do
        tar --strip-components=1 -C ${WORKDIR} \
            -xf $i
    done
}

SRC_URI_append_interface-diversity-sd-sd = " ${NXP_PROPRIETARY_DRIVER_LOCATION}/${NXP_PROPRIETARY_DRIVER_FILENAME};name=sd-sd-driver;subdir=archive.sd-sd "
SRC_URI[sd-sd-driver.sha256sum] = "${NXP_PROPRIETARY_DRIVER_SHA1}"
do_nxp_driver_unpack_interface-diversity-sd-sd() {
    DIRNAME=$(echo ${NXP_PROPRIETARY_DRIVER_FILENAME} | sed 's/\.zip//')
    DRVNAME=$(basename ${NXP_PROPRIETARY_DRIVER_FILENAME} | sed 's/zip/tar/')
    tar -C ${WORKDIR}/archive.sd-sd/ -xf ${WORKDIR}/archive.sd-sd/$DIRNAME/$DRVNAME
    for i in `ls ${WORKDIR}/archive.sd-sd/*-src.tgz`; do
        tar --strip-components=1 -C ${WORKDIR} \
            -xf $i
    done
}

SRC_URI_append_interface-diversity-pcie-usb = " ${NXP_PROPRIETARY_DRIVER_LOCATION}/${NXP_PROPRIETARY_DRIVER_FILENAME};name=pcie-usb-driver;subdir=archive.pcie-usb "
SRC_URI[pcie-usb-driver.sha256sum] = "${NXP_PROPRIETARY_DRIVER_SHA1}"
do_nxp_driver_unpack_interface-diversity-pcie-usb() {
    DRVNAME=$(basename ${NXP_PROPRIETARY_DRIVER_FILENAME} | sed 's/zip/tar/')
    tar -C ${WORKDIR}/archive.pcie-usb/ -xf ${WORKDIR}/archive.pcie-usb/$DRVNAME
    for i in `ls ${WORKDIR}/archive.pcie-usb/*-src.tgz`; do
        tar --strip-components=1 -C ${WORKDIR} \
            -xf $i
    done
}

