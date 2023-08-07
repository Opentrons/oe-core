FILESEXTRAPATHS_append := "${THISDIR}/files:"

SRC_URI += "file://opentrons-SetUp.ini"

do_install_append() {
  # install custom SetUp.ini
  install -d ${D}/home/root
  install -m 755 ${WORKDIR}/opentrons-SetUp.ini ${D}/home/root/SetUp.ini
}
