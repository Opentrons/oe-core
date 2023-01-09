SUMMARY = "Controller Area Network interface module for Python"
HOMEPAGE = "https://github.com/hardbyte/python-can"
LICENSE = "LGPLv3 & LGPL-3.0"
LIC_FILES_CHKSUM = "file://LICENSE.txt;md5=e6a600fd5e1d9cbde2d983680233ad02"

SRC_URI = "https://files.pythonhosted.org/packages/90/55/898e69e37d5d4692bf21ba8750e095493d2ecbb29be7394d5cb735f0ab0f/python-can-4.1.0.tar.gz"
SRC_URI[md5sum] = "d8365b7a09e49f47dac0da75d8518808"
SRC_URI[sha256sum] = "3f2b6b0dc5f459591d171ee0c0136dce79acedc2740ce695024aa3444e911bb9"

inherit setuptools3

RDEPENDS:${PN} += "python3-aenum python3-wrapt"

RDEPENDS:${PN} += "python3-asyncio python3-core python3-ctypes python3-curses python3-datetime python3-io python3-logging python3-math python3-multiprocessing python3-netclient python3-pickle python3-pkg-resources python3-pyserial python3-six python3-sqlite3 "
