SUMMARY = "Low-level Python CFFI Bindings for Argon2"
LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://LICENSE;md5=4642dfcbd13c1cc49e9f99df9de51ba1"

PYPI_ARCHIVE_NAME := "argon2_cffi_bindings-${PV}.tar.gz"
SRC_URI[sha256sum] = "b957f3e6ea4d55d820e40ff76f450952807013d361a65d7f28acc0acbf29229d"
FILESEXTRAPATHS:append = ":${THISDIR}/${PN}"
SRC_URI:append = " \
   file://0001-Return-to-PEP-621-license-specifier.patch \
   file://0002-Downgrade-setuptools-to-68.patch \
   "

inherit pypi python_setuptools_build_meta

S = "${WORKDIR}/argon2_cffi_bindings-${PV}"
DEPENDS += " ${PYTHON_PN}-setuptools-scm-native ${PYTHON_PN}-cffi-native argon2 "

ARCH = "unknown"
ARCH:arm = "arm"
ARCH:aarch64 = "aarch64"
ARCH:microblaze = "microblaze"
ARCH:x86_64 = "x86_64"
ARCH:x86 = "x86"

OS = "unknown"
OS:linux-gnu = "Linux"
OS:linux-gnueabi = "Linux"

export ARCHFLAGS = "-arch ${ARCH}"

do_configure:prepend() {
   sed -i -e 's,uname -m,echo ${ARCH},' -e 's,uname -s,echo ${OS},' ${S}/extras/libargon2/Makefile
}

RDEPENDS:${PN} = "argon2"
