SUMMARY = "Serialization library for Python to be used with remote objects"
HOMEPAGE = "https://github.com/irmen/Serpent"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://LICENSE;md5=d7c28f460fafe7be454fcdcac0b60263"

PV = "1.42"

SRC_URI[sha256sum] = "8ea082b01f8ba07ecd74e34a9118ac4521bc4594938d912b808c89f1da425506"

inherit pypi python_setuptools_build_meta

PYPI_PACKAGE="serpent"

RDEPENDS:${PN} += " python3-core "