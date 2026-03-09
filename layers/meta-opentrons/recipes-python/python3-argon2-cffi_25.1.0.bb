SUMMARY = "Secure ARgon2 password hashing algorithm"
LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://LICENSE;md5=e91e96ef55f45fe9caf7fc3e73672c4b"

PYPI_ARCHIVE_NAME := "argon2_cffi-${PV}.tar.gz"
SRC_URI[sha256sum] = "694ae5cc8a42f4c4e2bf2ca0e64e51e23a040c6a517a85074683d3959e1346c1"
FILESEXTRAPATHS:append = ":${THISDIR}/${PN}"
SRC_URI:append = " \
    file://0001-Remove-future-python-version-classifier.patch \
    file://0002-Return-to-PEP-621-license-specifier.patch \
    "

inherit pypi python_hatchling

S = "${WORKDIR}/argon2_cffi-${PV}"
RDEPENDS:${PN} = " ${PYTHON_PN}-argon2-cffi-bindings"
DEPENDS = " ${PYTHON_PN}-hatch-vcs-native ${PYTHON_PN}-hatch-fancy-pypi-readme-native "
