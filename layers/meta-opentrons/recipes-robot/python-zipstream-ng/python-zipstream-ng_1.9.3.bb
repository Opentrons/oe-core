
SUMMARY = "Python module stream zip encoded files"
HOMEPAGE = "https://github.com/pR0Ps/zipstream-ng"
LICENSE = "LGPL-3.0-only"

LIC_FILES_CHKSUM = "file://LICENSE;md5=c2cbad4c5fa2f803c5cfab86fae726b2"

PV = "1.9.3"

SRC_URI[sha256sum] = "6cebd055025699c0af594c76a9452cdf13f4be67ee005b6907f0d3c9c6f44ced"

inherit pypi python_setuptools_build_meta

PYPI_PACKAGE="zipstream-ng"
