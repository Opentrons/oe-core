SUMMARY = "Simple, asyncio-based inotify library for Python."
HOMEPAGE = "https://github.com/rbarrois/aionotify"
LICENSE = "BSD"

LIC_FILES_CHKSUM = "file://LICENSE;md5=95348062da1183a702f2bfba094b9952"
SRC_URI = "https://files.pythonhosted.org/packages/57/c0/237d434870a024a16d89751ec9f4e5340a6d84d49ccb1ab738b3fdee907a/aionotify-${PV}.tar.gz"
SRC_URI[md5sum] = "252066f7ab85aa71a89d23d3e42ce041"
SRC_URL[sha256sum] = "64b702ad0eb115034533f9f62730a9253b79f5ff0fbf3d100c392124cdf12507"
S = "${WORKDIR}/aionotify-${PV}"

inherit setuptools3

