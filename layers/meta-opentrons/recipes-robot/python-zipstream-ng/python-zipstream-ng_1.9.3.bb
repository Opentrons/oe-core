SUMMARY = "Python module stream zip encoded files"
HOMEPAGE = "https://github.com/pR0Ps/zipstream-ng"
LICENSE = "LGPL-3.0-only"

LIC_FILES_CHKSUM = "file://LICENSE;md5=3000208d539ec061b899bce1d9ce9404"

PYPI_SRC_URI = "https://files.pythonhosted.org/packages/f3/e0/76e5a674b0f6a6b7f90af9b87a92fbe1f2b97da447fb09078bcba6f6a28e/zipstream_ng-1.9.3.tar.gz"
SRC_URI[sha256sum] = "6cebd055025699c0af594c76a9452cdf13f4be67ee005b6907f0d3c9c6f44ced"

inherit pypi python_setuptools_build_meta python_hatchling

PYPI_PACKAGE="python-zipstream-ng"
S = "${WORKDIR}/zipstream_ng-1.9.3"
