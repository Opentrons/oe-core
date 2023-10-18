SUMMARY = "Controller Area Network interface module for Python"
HOMEPAGE = "https://github.com/hardbyte/python-can"
LICENSE = "LGPLv3 & LGPL-3.0"
LIC_FILES_CHKSUM = "file://LICENSE.txt;md5=e6a600fd5e1d9cbde2d983680233ad02"

SRC_URI = "https://files.pythonhosted.org/packages/97/dd/5e5ae96db41ba57dde127e0600c3d324239ed692e167296c5fdb992cbf41/python-can-${PV}.tar.gz"
SRC_URI[md5sum] = "305075968c56bd85130b19d86e9e505d"
SRC_URI[sha1sum] = "2092a5293a8bf387d33d7d9557a00b4a2b7418aa"
SRC_URI[sha256sum] = "2d3c223b7adc4dd46ce258d4a33b7e0dbb6c339e002faa40ee4a69d5fdce9449"
SRC_URI[sha384sum] = "1014dd8ed9f7966d3e766d54ac1e650368bddcb6dd345257d7297ddb2dd941dfb87efd8969942ac16794611601cba5e3"
SRC_URI[sha512sum] = "cc4084c0348e1e0ef2372ebd863f2dfc85cae7315beb15d68f6e7d9531f05318d4307624bca13b3ae711ce219d3434183cd14d758d6073bf03db69d347eb7e6c"

inherit setuptools3

RDEPENDS:${PN} += "python3-aenum python3-wrapt"

RDEPENDS:${PN} += "python3-asyncio python3-core python3-ctypes python3-curses python3-datetime python3-io python3-logging python3-math python3-multiprocessing python3-netclient python3-pickle python3-pkg-resources python3-pyserial python3-six python3-sqlite3 python3-stringold python3-threading "
