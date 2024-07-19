DESCRIPTION = "psutil is a cross-platform library for retrieving information on running processes and system utilization (CPU, memory, disks, network, sensors) in Python."
HOMEPAGE = "https://github.com/giampaolo/psutil"
SECTION = "devel/python"
LICENSE = "BSD-3-Clause"
LIC_FILES_CHKSUM = "file://LICENSE;md5=a9c72113a843d0d732a0ac1c200d81b1"

SRC_URI[sha256sum] = "187588c10ff4804b91e0c947d6b1a4006dbb633261c0f869865de518603c5d5e"
SRC_URI = "https://github.com/giampaolo/psutil/archive/refs/tags/release-6.0.0.tar.gz"

S = "${WORKDIR}/psutil-release-6.0.0"

inherit pypi setuptools3

RDEPENDS_${PN} += "python3"

FILES_${PN} += "${libdir}/python3*/site-packages/psutil"
