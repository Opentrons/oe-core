require u-boot-common-2022.07.inc
require u-boot.2022.07.inc

LIC_FILES_CHKSUM = "file://Licenses/README;md5=2ca5f2c35c8cc335f0a19756634782f1"
DEPENDS += "bc-native dtc-native python3-setuptools-native"

# hash of release v2022.07"
PV = "2022.07"
SRCREV = "e092e3250270a1016c877da7bdd9384f14b1321e"
# patches which are not (yet) in the used stable version
TDX_PATCHES = " \
    file://0001-toradex-tdx-cfg-block-use-only-snprintf.patch \
    file://0002-toradex-tdx-cfg-block-use-defines-for-string-length.patch \
    file://0003-toradex-tdx-cfg-block-extend-assembly-version.patch \
    file://0004-toradex-tdx-cfg-block-add-new-toradex-oui-range.patch \
    file://0005-toradex-tdx-cfg-block-add-0068-i.mx-8m-mini-sku.patch \
    file://0006-toradex-common-Remove-stale-comments-about-modules-a.patch \
    file://0007-toradex-common-Use-ARRAY_SIZE-macro.patch \
    file://0008-toradex-tdx-cfg-block-Cleanup-interactive-cfg-block-.patch \
    file://0009-toradex-common-Remove-stale-function-declaration.patch \
    file://0010-toradex-common-Remove-ifdef-usage-for-2nd-ethaddr.patch \
    file://0011-toradex-tdx-cfg-block-Use-official-SKU-names.patch \
    file://0012-toradex-common-Improve-product-serial-print-during-b.patch \
    file://0013-configs-colibri-imx7-Enable-bootd-command.patch \
    file://0001-ARM-imx8mp-verdin-imx8mp-Add-memory-size-detection.patch \
    file://0001-apalis-colibri_imx6-imx6ull-_imx7-update-env-memory-.patch \
    file://0001-configs-colibri-imx7-Fix-bad-block-table-in-flash-co.patch \
    file://0001-colibri_imx6-fix-RALAT-and-WALAT-values.patch \
"
SRC_URI:append = " ${TDX_PATCHES}"
SRC_URI:append:use-nxp-bsp:colibri-imx7 = " \
    file://0001-colibri_imx7-boot-linux-kernel-in-secure-mode.patch \
"

inherit toradex-u-boot-localversion

UBOOT_INITIAL_ENV = "u-boot-initial-env"

PADDING_DIR = "${B}"
nand_padding () {
    # pad the end of U-Boot with 0x00 up to the the end of the CSF area
    #PAD_END=$(echo -n "0x"; od -X  -j 0x24 -N 4 u-boot.imx | sed -e '/................/!d' -e 's/........\(.*\)/\1/')
    #PAD_END=$(( $PAD_END - 0x400 ))
    #objcopy -I binary -O binary --pad-to $PAD_END u-boot.imx u-boot.imx.zero-padded
    # assume that the above never need more than 10k of padding and skip the
    # shell magic to get a correct size.
    dd bs=10k count=1 if=/dev/zero | cat ${PADDING_DIR}/u-boot.imx - > ${PADDING_DIR}/u-boot.imx.zero-padded

    # U-Boot is flashed 1k into a NAND block, create a binary which prepends
    # U-boot with 1k of zeros to ease flashing
    dd bs=1024 count=1 if=/dev/zero | cat - ${PADDING_DIR}/u-boot.imx.zero-padded > ${PADDING_DIR}/u-boot-nand.imx
}

deploy_uboot_with_spl () {
    #Deploy u-boot-with-spl.imx
    if [ -n "${UBOOT_CONFIG}" ]
    then
        for config in ${UBOOT_MACHINE}; do
            i=$(expr $i + 1);
            for type in ${UBOOT_CONFIG}; do
                j=$(expr $j + 1);
                if [ $j -eq $i ]
                then
                    install -D -m 644 ${B}/${config}/u-boot-with-spl.imx ${DEPLOYDIR}/u-boot-with-spl.imx-${MACHINE}-${type}
                    ln -sf u-boot-with-spl.imx-${MACHINE}-${type} ${DEPLOYDIR}/u-boot-with-spl.imx
                fi
            done
            unset  j
        done
        unset  i
    else
        install -D -m 644 ${B}/${config}/u-boot-with-spl.imx ${DEPLOYDIR}/u-boot-with-spl.imx
    fi
}

# build imx-boot from within U-Boot
inherit ${@oe.utils.ifelse(d.getVar('UBOOT_PROVIDES_BOOT_CONTAINER') == '1', 'imx-boot-container', '')}
DEPENDS:imx-boot-container += "bc-native bison-native dtc-native python3-setuptools-native swig-native"

do_compile:append:colibri-imx6ull () {
    nand_padding
}

do_compile:append:colibri-imx7 () {
    nand_padding
}

do_deploy:append:colibri-imx6 () {
    deploy_uboot_with_spl
}

do_deploy:append:apalis-imx6 () {
    deploy_uboot_with_spl
}

BOOT_TOOLS = "imx-boot-tools"
do_deploy:append:mx8m-generic-bsp() {
    # Deploy u-boot-nodtb.bin and fsl-imx8m*-XX.dtb for mkimage to generate boot binary
    if [ -n "${UBOOT_CONFIG}" ]
    then
        for config in ${UBOOT_MACHINE}; do
            i=$(expr $i + 1);
            for type in ${UBOOT_CONFIG}; do
                j=$(expr $j + 1);
                if [ $j -eq $i ]
                then
                    install -d ${DEPLOYDIR}/${BOOT_TOOLS}
                    install -m 0777 ${B}/${config}/arch/arm/dts/${UBOOT_DTB_NAME}  ${DEPLOYDIR}/${BOOT_TOOLS}
                    install -m 0777 ${B}/${config}/u-boot-nodtb.bin  ${DEPLOYDIR}/${BOOT_TOOLS}/u-boot-nodtb.bin-${MACHINE}-${type}
                fi
            done
            unset  j
        done
        unset  i
    fi
}

do_deploy:append:imx-boot-container() {
    # Deploy imx-boot
    if [ -n "${UBOOT_CONFIG}" ]
    then
        for config in ${UBOOT_MACHINE}; do
            i=$(expr $i + 1);
            for type in ${UBOOT_CONFIG}; do
                j=$(expr $j + 1);
                if [ $j -eq $i ]
                then
                    install -d ${DEPLOYDIR}
                    install -m 0644 ${B}/${config}/flash.bin ${DEPLOYDIR}/imx-boot-${MACHINE}-${type}
                    ln -sf imx-boot-${MACHINE}-${type} ${DEPLOYDIR}/imx-boot
                fi
            done
            unset  j
        done
        unset  i
    fi
}
