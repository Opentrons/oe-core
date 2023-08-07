SUMMARY = "Scripts for testing"
LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/Apache-2.0;md5=89aea4e17d99a7cacdbeed46a0096b10"

RDEPENDS_${PN} += "bash"
FILESEXTRAPATHS_prepend := "${THISDIR}/files:"

SRC_URI += "\
           file://switch-wifi-mode.sh \
           file://wifi-mode.sh \
           "

do_install() {
   install -d ${D}/home/root
   install -m 755 ${WORKDIR}/switch-wifi-mode.sh ${D}/home/root/switch-wifi-mode.sh

   # install prodile.d
   install -d ${D}${sysconfdir}/profile.d/
   install -m 755 ${WORKDIR}/wifi-mode.sh ${D}${sysconfdir}/profile.d/wifi-mode.sh
}

FILES_${PN} += "\
               /home/root/switch-wifi-mode.sh \
               ${sysconfdir}/profile.d/wifi-mode.sh \
               "
