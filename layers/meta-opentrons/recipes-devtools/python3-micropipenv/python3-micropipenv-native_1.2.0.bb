SUMMARY = "A simple wrapper around pip to support requirements.txt, Pipenv and Poetry files for containerized applications"
LICENSE = " LGPL-3.0-only"
HOMEPAGE = "https://github.com/thoth-station/micropipenv"
LIC_FILES_CHKSUM = "file://LICENSE-LGPL;md5=3000208d539ec061b899bce1d9ce9404"

SRC_URI[sha256sum] = "275e84b3e8227fb585a050ce4f333539ed1096e76b09340649abca475d3151ca"
SRC_URI[md5sum] = "c5f390bcd97d7089e26099dd746ff838"
S = "${WORKDIR}/micropipenv-${PV}"

DEPENDS = "python3-native python3-toml-native "

inherit setuptools3 pypi native
