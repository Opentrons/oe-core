SUMMARY = "AVR udev rules allow unpriviledged users acces to AVR dev tools"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COREBASE}/meta/COPYING.MIT;md5=3da9cfbcb788c80a0384361b4de20420"

inherit allarch

SRC_URI = "file://60-avr-dev-devices.rules"
PV = "0.1"

do_install () {
    install -d ${D}${sysconfdir}/udev/rules.d
    install -m 0644 ${WORKDIR}/60-avr-dev-devices.rules ${D}${sysconfdir}/udev/rules.d/
}
