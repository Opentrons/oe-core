SUMMARY = "JSON Web Token implementation in Python"
DESCRIPTION = "A Python implementation of JSON Web Token draft 32.\
 Original implementation was written by https://github.com/progrium"
HOMEPAGE = "http://github.com/jpadilla/pyjwt"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://LICENSE;md5=68626705a7b513ca8d5f44a3e200ed0c"

SRC_URI[md5sum] = "aeed6d3a581ae383b2288a2079fa562d"
SRC_URI[sha256sum] = "69285c7e31fc44f68a1feb309e948e0df53259d579295e6cfe2b1792329f05fd"

PYPI_PACKAGE = "PyJWT"
inherit pypi setuptools3

RDEPENDS_${PN} = "${PYTHON_PN}-cryptography"

BBCLASSEXTEND = "native nativesdk"
