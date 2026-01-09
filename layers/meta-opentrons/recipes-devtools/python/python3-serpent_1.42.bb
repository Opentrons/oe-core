SUMMARY = "Serialization library for Python to be used with remote objects"
HOMEPAGE = "https://github.com/irmen/Serpent"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://LICENSE;md5=8f3b1c4e4e2f2b1766d6a3f4b0e0e0e8"

PV = "1.42"
inherit python3native python3-wheel python3-setuptools

PYPI_PACKAGE = "serpent"
SRC_URI[sha256sum] = "8ea082b01f8ba07ecd74e34a9118ac4521bc4594938d912b808c89f1da425506"

S = "${WORKDIR}/${PYPI_PACKAGE}-${PV}"

RDEPENDS:${PN} += "python3-core"