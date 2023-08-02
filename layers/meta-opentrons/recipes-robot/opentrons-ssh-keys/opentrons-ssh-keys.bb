DESCRIPTION = "Install authorized SSH keys to the image"
LICENSE="Apache-2.0"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/Apache-2.0;md5=89aea4e17d99a7cacdbeed46a0096b10"

SRC_URI = "file://opentrons-flex.pub"

do_install () {
    install -m 700 -d ${D}/home/root/.ssh
    install -m 644 ${WORKDIR}/opentrons-flex.pub ${D}/home/root/.ssh/authorized_keys
}

FILES_${PN} += "/home/root/.ssh/authorized_keys \
"
