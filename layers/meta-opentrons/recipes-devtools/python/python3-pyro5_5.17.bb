
SUMMARY = "Python module to supply Python Remote Objects"
HOMEPAGE = "https://github.com/irmen/Pyro5"
LICENSE = "MIT"

LIC_FILES_CHKSUM = "file://LICENSE;md5=c1c9ccd5f4ca5d0f5057c0e690a0153d"

PV = "5.17"

SRC_URI[sha256sum] = "cfac69638d80943aff9cc5f1466755dd0fef8aed0bb4bda41b5eb045818ce6fc"

inherit pypi python_setuptools_build_meta

PYPI_PACKAGE="pyro5"

RDEPENDS:${PN} += " python3-serpent "
