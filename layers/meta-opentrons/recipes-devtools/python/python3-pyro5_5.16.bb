
SUMMARY = "Python module to supply Python Remote Objects"
HOMEPAGE = "https://github.com/irmen/Pyro5"
LICENSE = "MIT"

LIC_FILES_CHKSUM = "file://LICENSE;md5=c1c9ccd5f4ca5d0f5057c0e690a0153d"

PV = "5.16"
PYPI_PACKAGE = "Pyro5"
SRC_URL = "https://files.pythonhosted.org/packages/75/4d/7713c06c1a9eaa35142e232e89da5300cac09156977204c730b28f37eaec/pyro5-5.16-py3-none-any.whl"
SRC_URI[sha256sum] = "ea33cad29993fd44ce394ef45950e8dc5805ee12c4dd76541a8dd11c40694706"

S = "${WORKDIR}/${PYPI_PACKAGE}-${PV}"

inherit pypi python_setuptools_build_meta

DEPENDS += "python3-setuptools-scm-native python3-wheel-native"

RDEPENDS:${PN} += " \
    python3-serpent \
    python3-setuptools \
    python3-setuptools-scm \
    python3-wheel \
"

BBCLASSEXTEND = "native nativesdk"