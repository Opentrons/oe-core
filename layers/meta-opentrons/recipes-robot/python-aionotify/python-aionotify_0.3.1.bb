SUMMARY = "Simple, asyncio-based inotify library for Python."
HOMEPAGE = "https://github.com/rbarrois/aionotify"
LICENSE = "BSD-2-Clause"

LIC_FILES_CHKSUM = "file://LICENSE;md5=95348062da1183a702f2bfba094b9952"
SRC_URI = "https://files.pythonhosted.org/packages/23/16/81a26a64d728e76eea073cd0316f3e8885cca312247a9ba9af64d7c47e64/aionotify-${PV}.tar.gz"
SRC_URI[md5sum] = "8df747c66eb0c0f0567756d90569457d"
SRC_URI[sha256sum] = "9651e1373873c75786101330e302e114f85b6e8b5ad70b491497c8b3609a8449"
S = "${WORKDIR}/aionotify-${PV}"

inherit setuptools3
