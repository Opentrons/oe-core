SUMMARY = "Serialization library for Python to be used with remote objects"
HOMEPAGE = "https://github.com/irmen/Serpent"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://LICENSE;md5=8f3b1c4e4e2f2b1766d6a3f4b0e0e0e8"

PV = "1.42"

PYPI_PACKAGE = "serpent"
SRC_URI = "https://files.pythonhosted.org/packages/07/24/73031e6bd25d8f94811b3752b0b217efbdb20a67b65c6838c9af4e50c2e2/serpent-1.42-py3-none-any.whl"
SRC_URI[sha256sum] = "8ea082b01f8ba07ecd74e34a9118ac4521bc4594938d912b808c89f1da425506"

S = "${WORKDIR}/${PYPI_PACKAGE}-${PV}"

inherit pypi python_setuptools_build_meta

DEPENDS += "python3-setuptools-scm-native python3-wheel-native"

RDEPENDS:${PN} += " \
    python3-core \
    python3-setuptools \
    python3-setuptools-scm \
    python3-wheel \
"

BBCLASSEXTEND = "native nativesdk"