# Note: this is from upstream versions of openembedded-core. evaluate for removal
# after more upgrades.
SUMMARY = "Download, build, install, upgrade, and uninstall Python packages"
HOMEPAGE = "https://pypi.org/project/setuptools"
SECTION = "devel/python"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://LICENSE;md5=141643e11c48898150daa83802dbc65f"

inherit pypi python_setuptools_build_meta

SRC_URI:append:class-native = " file://0001-conditionally-do-not-fetch-code-by-easy_install.patch"

SRC_URI += " \
            file://0001-_distutils-sysconfig.py-make-it-possible-to-substite.patch"

SRC_URI[sha256sum] = "4ac1475276d2f1c48684874089fefcd83bd7162ddaafb81fac866ba0db282a87"

DEPENDS += "${PYTHON_PN}"

RDEPENDS:${PN} = "\
    ${PYTHON_PN}-2to3 \
    ${PYTHON_PN}-compile \
    ${PYTHON_PN}-compression \
    ${PYTHON_PN}-ctypes \
    ${PYTHON_PN}-email \
    ${PYTHON_PN}-html \
    ${PYTHON_PN}-json \
    ${PYTHON_PN}-netserver \
    ${PYTHON_PN}-numbers \
    ${PYTHON_PN}-pickle \
    ${PYTHON_PN}-pkg-resources \
    ${PYTHON_PN}-pkgutil \
    ${PYTHON_PN}-plistlib \
    ${PYTHON_PN}-shell \
    ${PYTHON_PN}-stringold \
    ${PYTHON_PN}-threading \
    ${PYTHON_PN}-unittest \
    ${PYTHON_PN}-xml \
"

BBCLASSEXTEND = "native nativesdk"

# The pkg-resources module can be used by itself, without the package downloader
# and easy_install. Ship it in a separate package so that it can be used by
# minimal distributions.
PACKAGES =+ "${PYTHON_PN}-pkg-resources "
FILES:${PYTHON_PN}-pkg-resources = "${PYTHON_SITEPACKAGES_DIR}/pkg_resources/*"
RDEPENDS:${PYTHON_PN}-pkg-resources = "\
    ${PYTHON_PN}-compression \
    ${PYTHON_PN}-email \
    ${PYTHON_PN}-plistlib \
    ${PYTHON_PN}-pprint \
"

# This used to use the bootstrap install which didn't compile. Until we bump the
# tmpdir version we can't compile the native otherwise the sysroot unpack fails
INSTALL_WHEEL_COMPILE_BYTECODE:class-native = "--no-compile-bytecode"
