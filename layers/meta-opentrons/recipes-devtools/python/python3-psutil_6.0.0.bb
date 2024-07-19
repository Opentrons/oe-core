DESCRIPTION = "psutil is a cross-platform library for retrieving information on running processes and system utilization (CPU, memory, disks, network, sensors) in Python."
HOMEPAGE = "https://github.com/giampaolo/psutil"
SECTION = "devel/python"
LICENSE = "BSD-3-Clause"
LIC_FILES_CHKSUM = "file://LICENSE;md5=d2ac07f3999abbbb82c4c1a5397a2b27"

SRC_URI[sha256sum] = "9e4c75b17b7c4e40b0d3f6e3e44dc9f9a23e5468ae75c4c28f42d3717cfc2580"
SRC_URI = "https://github.com/giampaolo/psutil/archive/refs/tags/release-6.0.0.tar.gz"

S = "${WORKDIR}/psutil-release-6.0.0"

inherit pypi setuptools3

RDEPENDS_${PN} += "python3"

FILES_${PN} += "${libdir}/python3*/site-packages/psutil"
