# Override the version of setuptools in openembedded-core to work around
# https://github.com/pypa/setuptools/issues/3118 . This doesn't use the
# most recent version of setuptools because it in turn isn't quite
# compatible with the way the overrided recipe installs it.

require recipes-devtools/python/python-setuptools.inc
SRC_URI[md5sum] = "e2c6c3d64b38efe2e81bcd502cf54dcc"
SRC_URI[sha256sum] = "4334fc63121aafb1cc98fd5ae5dd47ea8ad4a38ad638b47af03a686deb14ef5b"
SRC_URI:remove:class-native = " file://0001-conditionally-do-not-fetch-code-by-easy_install.patch"
inherit setuptools3


do_install:append() {
    mv ${D}${bindir}/easy_install ${D}${bindir}/easy3_install
}
