HOMEPAGE = "http://mesonbuild.com"
SUMMARY = "A high performance build system"
DESCRIPTION = "Meson is a build system designed to increase programmer \
productivity. It does this by providing a fast, simple and easy to use \
interface for modern software development tools and practices."

LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://COPYING;md5=3b83ef96387f14655fc854ddc3c6bd57"

SRC_URI = "https://github.com/mesonbuild/meson/releases/download/${PV}/meson-${PV}.tar.gz \
           file://0001-gtkdoc-fix-issues-that-arise-when-cross-compiling.patch \
           file://0003-native_bindir.patch \
           file://0001-python-module-do-not-manipulate-the-environment-when.patch \
           file://disable-rpath-handling.patch \
           file://cross-prop-default.patch \
           file://0001-modules-python.py-do-not-substitute-python-s-install.patch \
           file://0001-gnome.py-prefix-g-i-paths-with-PKG_CONFIG_SYSROOT_DI.patch \
           "
SRC_URI[sha256sum] = "6bed2a25a128bbabe97cf40f63165ebe800e4fcb46db8ab7ef5c2b5789f092a5"
SRC_URI[md5sum] ="99777acd77838c8669e1421fda8e31c5"

SRC_URI:append:class-native = " \
    file://0001-Make-CPU-family-warnings-fatal.patch \
    file://0002-Support-building-allarch-recipes-again.patch \
"

UPSTREAM_CHECK_URI = "https://github.com/mesonbuild/meson/releases"
UPSTREAM_CHECK_REGEX = "meson-(?P<pver>\d+(\.\d+)+)\.tar"

inherit setuptools3

RDEPENDS:${PN} = "ninja python3-modules python3-pkg-resources"

FILES:${PN} += "${datadir}/polkit-1"

BBCLASSEXTEND = "native"
