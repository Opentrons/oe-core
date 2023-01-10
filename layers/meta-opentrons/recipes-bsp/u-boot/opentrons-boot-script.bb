DESCRIPTION = "Boot script for Opentrons boot process"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COREBASE}/meta/COPYING.MIT;md5=3da9cfbcb788c80a0384361b4de20420"

INHIBIT_DEFAULT_DEPS = "1"
DEPENDS = "u-boot-mkimage-native"

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"
SRC_URI = " \
    file://boot.cmd.in \
"

KERNEL_BOOTCMD ??= "bootz"
KERNEL_BOOTCMD:aarch64 ?= "booti"

inherit deploy

do_deploy() {
    sed -e 's/@@KERNEL_BOOTCMD@@/${KERNEL_BOOTCMD}/;s/@@KERNEL_IMAGETYPE@@/${KERNEL_IMAGETYPE}/' \
        "${WORKDIR}/boot.cmd.in" > boot.cmd
    mkimage -T script -C none -n "Opentrons boot script" -d boot.cmd boot.scr

    install -m 0644 boot.scr ${DEPLOYDIR}/boot.scr-${MACHINE}
}

addtask deploy after do_install before do_build

PROVIDES += "u-boot-default-script"

PACKAGE_ARCH = "${MACHINE_ARCH}"
