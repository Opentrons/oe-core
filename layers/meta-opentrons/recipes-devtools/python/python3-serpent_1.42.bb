SUMMARY = "Serialization library for Python to be used with remote objects"
HOMEPAGE = "https://github.com/irmen/Serpent"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://LICENSE;md5=d7c28f460fafe7be454fcdcac0b60263"

PV = "1.42"

SRC_URI = "git://github.com/irmen/Serpent.git;protocol=https;branch=master"
SRCREV = "f6cb65e4301bc424a3de2c894f88c7537190abba"

S = "${WORKDIR}/git"

inherit python_setuptools_build_meta

DEPENDS += "python3-setuptools-scm-native python3-wheel-native"

RDEPENDS:${PN} += " \
    python3-core \
    python3-setuptools \
    python3-setuptools-scm \
    python3-wheel \
"

BBCLASSEXTEND = "native nativesdk"