SUMMARY = "Serialization library for Python to be used with remote objects"
HOMEPAGE = "https://github.com/irmen/Serpent"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://LICENSE;md5=d7c28f460fafe7be454fcdcac0b60263"

PV = "1.43"

SRC_URI[sha256sum] = "62dc242fd4ea2a50339f4f5aaaf6ecc55605ee74770d7eb2031e760d90a0d114"

inherit pypi python_setuptools_build_meta

PYPI_PACKAGE="serpent"

RDEPENDS:${PN} += " python3-core "