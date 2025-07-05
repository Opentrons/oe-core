# Copyright (c) 2012-2014 LG Electronics, Inc.
SUMMARY = "c-ares is a C library that resolves names asynchronously."
HOMEPAGE = "http://daniel.haxx.se/projects/c-ares/"
SECTION = "libs"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://LICENSE.md;md5=fb997454c8d62aa6a47f07a8cd48b006"

PV = "1.17.1"

SRC_URI = "\
    https://github.com/c-ares/c-ares.git \
    file://cmake-install-libcares.pc.patch \
    file://0001-fix-configure-error-mv-libcares.pc.cmakein-to-libcar.patch \
"
SRC_URI[sha256sum] = "7b836bd45376d86f5e78bc64f39d203fdf6dfa00a3dc66cf288db9bde3d34a85"
SRCREV = "39c73b503d9ef70a58ad1f4a4643f15b01407c66"

UPSTREAM_CHECK_GITTAGREGEX = "cares-(?P<pver>\d+_(\d_?)+)"

S = "${WORKDIR}/git"

inherit cmake pkgconfig

PACKAGES =+ "${PN}-utils"

FILES_${PN}-utils = "${bindir}"

BBCLASSEXTEND = "native nativesdk"
