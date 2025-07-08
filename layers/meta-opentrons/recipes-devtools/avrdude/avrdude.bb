SUMMARY = "AVRDUDE - AVR Downloader/UploaDEr"
HOMEPAGE = "https://www.nongnu.org/avrdude/"
SECTION = "devel"
LICENSE = "GPL-2.0-or-later"

LIC_FILES_CHKSUM = "file://COPYING;md5=4f51bb496ef8872ccff73f440f2464a8"

inherit autotools gettext

# Do we need libhid?
DEPENDS = " \
    bison-native \
    flex \
    elfutils \
    libusb1 \
    libftdi \
    hidapi \
"

SRC_URI = "git://github.com/avrdudes/avrdude.git;protocol=https;branch=main"
SRCREV = "f36484ed192369846e3af8a9c4cbc0352096ec84"
S = "${WORKDIR}/git"
PV = "6.3+git${SRCPV}"

RRECOMMENDS:${PN} += "avr-udev-rules"
