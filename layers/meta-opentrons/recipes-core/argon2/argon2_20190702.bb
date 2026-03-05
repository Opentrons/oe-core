# SPDX-License-Identifier: MIT
# Copyright (C) 2022 iris-GmbH infrared & intelligent sensors

SUMMARY = "C reference implementation of the Argon2 password-hashing function"

DESCRIPTION = "Argon2 is a password-hashing function that summarizes the state of the art in the design of memory-hard functions and can be used to hash passwords for credential storage, key derivation, or other applications."

LICENSE = "CC0-1.0 | Apache-2.0"
LIC_FILES_CHKSUM = "file://LICENSE;md5=3f577d9446ded914cd9f8f0ae099a45a"

SECTION = "libs cli"

SRC_URI = "git://github.com/P-H-C/phc-winner-argon2.git;protocol=https;branch=master"
SRC_URI += "file://FindArgon2.cmake"
SRC_URI += "file://0001-Makefile-Remove-native-optimization-check.patch"

SRCREV = "62358ba2123abd17fccf2a108a301d4b52c01a7c"

S = "${WORKDIR}/git"

do_compile () {
    oe_runmake
}

do_install () {
    oe_runmake install DESTDIR=${D} LIBRARY_REL=lib
    install -d ${D}${datadir}/cmake/Modules
    install -m 644 ${WORKDIR}/FindArgon2.cmake ${D}${datadir}/cmake/Modules
}

PACKAGES =+ "${PN}-bin"
FILES:${PN}-bin = "${bindir}/*"

BBCLASSEXTEND = "native nativesdk"
