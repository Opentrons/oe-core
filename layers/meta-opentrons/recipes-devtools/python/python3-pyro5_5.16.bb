
SUMMARY = "Python module to supply Python Remote Objects"
HOMEPAGE = "https://github.com/irmen/Pyro5"
LICENSE = "MIT"

LIC_FILES_CHKSUM = "file://LICENSE;md5=c1c9ccd5f4ca5d0f5057c0e690a0153d"

PV = "5.16"
inherit python3native python3-setuptools

PYPI_PACKAGE = "Pyro5"
SRC_URI[sha256sum] = "d40418ed2acee0d9093daf5023ed0b0cb485a6b62342934adb9e801956f5738b"

S = "${WORKDIR}/${PYPI_PACKAGE}-${PV}"

RDEPENDS:${PN} += " \
    python3-serpent \
    python3-wheel \
"