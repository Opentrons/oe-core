SUMMARY = "Controller Area Network interface module for Python"
HOMEPAGE = "https://github.com/hardbyte/python-can"
LICENSE = "LGPLv3 & LGPL-3.0"
LIC_FILES_CHKSUM = "file://LICENSE.txt;md5=e6a600fd5e1d9cbde2d983680233ad02"

SRC_URI = "https://files.pythonhosted.org/packages/dd/f1/327caaf05b6bca594250053058a2adac537a88dfb5c41bb5498cfda9de78/python-can-4.2.2.tar.gz"
SRC_URI[sha256sum] = "6ad50f4613289f3c4d276b6d2ac8901d776dcb929994cce93f55a69e858c595f"

inherit python_setuptools_build_meta

RDEPENDS:${PN} += "python3-wrapt python3-typing-extensions python3-msgpack python3-packaging"

