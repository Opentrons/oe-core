# By default, udev rules in udev-extraconf (?) accidentally pick up
# /dev/mmcblk0 as removable media and automount it. This is dangerous
# because it includes the boot partition and standby root partition.
# This recipe adds it to the udev ignorelist.

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI += "file://mmcblk0.ignorelist"

do_install:append() {
    install -m 0644 ${WORKDIR}/mmcblk0.ignorelist ${D}${sysconfdir}/udev/mount.ignorelist.d/mmcblk0.ignorelist
}
