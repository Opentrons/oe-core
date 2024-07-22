SUMMARY = "A cross-platform library for retrieving information on running processes and system utilization (CPU, memory, disks, network, sensors) in Python."
DESCRIPTION = "psutil is a cross-platform library for retrieving information on running processes and system utilization (CPU, memory, disks, network, sensors) in Python."
HOMEPAGE = "https://github.com/giampaolo/psutil"
SECTION = "devel/python"
LICENSE = "BSD-3-Clause"
LIC_FILES_CHKSUM = "file://LICENSE;md5=a9c72113a843d0d732a0ac1c200d81b1"

SRC_URI[sha256sum] = "8faae4f310b6d969fa26ca0545338b21f73c6b15db7c4a8d934a5482faa818f2"
SRC_URI = "https://files.pythonhosted.org/packages/source/p/psutil/psutil-${PV}.tar.gz"

PYPI_PACKAGE = "psutil"

inherit pypi python_setuptools_build_meta

DEPENDS += "${PYTHON_PN}-setuptools-native"

RDEPENDS_${PN} += " \
    ${PYTHON_PN}-core \
    ${PYTHON_PN}-distutils \
    ${PYTHON_PN}-shell \
    ${PYTHON_PN}-netclient \
    ${PYTHON_PN}-numbers \
    ${PYTHON_PN}-mmap \
    ${PYTHON_PN}-contextlib \
    ${PYTHON_PN}-datetime \
    ${PYTHON_PN}-pprint \
    ${PYTHON_PN}-logging \
    ${PYTHON_PN}-math \
    ${PYTHON_PN}-resource \
    ${PYTHON_PN}-fcntl \
    ${PYTHON_PN}-pickle \
"

BBCLASSEXTEND = "native nativesdk"