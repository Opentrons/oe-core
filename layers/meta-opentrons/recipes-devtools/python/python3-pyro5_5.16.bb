
SUMMARY = "Python module to supply Python Remote Objects"
HOMEPAGE = "https://github.com/irmen/Pyro5"
LICENSE = "MIT"

LIC_FILES_CHKSUM = "file://LICENSE;md5=c1c9ccd5f4ca5d0f5057c0e690a0153d"

PV = "5.16"

SRC_URI = "git://github.com/irmen/Pyro5.git;protocol=https;branch=master"
SRCREV = "66f010a62050fc29c52303dce3ac89879b4a8581"

S = "${WORKDIR}/git"

inherit python_setuptools_build_meta

DEPENDS += "python3-setuptools-scm-native python3-wheel-native"

RDEPENDS:${PN} += " \
    python3-serpent \
    python3-setuptools \
    python3-setuptools-scm \
    python3-wheel \
"

BBCLASSEXTEND = "native nativesdk"