SUMMARY = "pip is the package installer for Python."
LICENSE = "MIT"
HOMEPAGE = "https://pip.pypa.io/"
LIC_FILES_CHKSUM = "file://LICENSE.txt;md5=c4fa2b50f55649f43060fa04b0919b9b"

SRC_URI[sha256sum] = "0eb8a1516c3d138ae8689c0c1a60fde7143310832f9dc77e11d8a4bc62de193b"
SRC_URI[md5sum] = "efbdb4201a5e6383fb4d12e26f78f355"
S = "${WORKDIR}/pip-${PV}"

DEPENDS = "python3-native "

inherit native setuptools3 pypi
