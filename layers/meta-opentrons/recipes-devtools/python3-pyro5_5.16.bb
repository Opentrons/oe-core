
SUMMARY = "Python module to supply Python Remote Objects"
HOMEPAGE = "https://github.com/irmen/Pyro5"
LICENSE = "MIT"

LIC_FILES_CHKSUM = "file://LICENSE;md5=c1c9ccd5f4ca5d0f5057c0e690a0153d"

#S = "${WORKDIR}/Pyro5-${PV}"

inherit pypi setuptools3

PYPI_PACKAGE = "Pyro5"
# SRC_URI[md5sum] = "4e487f3e16667484025416fc687dc858"
SRC_URL[sha256sum] = "d40418ed2acee0d9093daf5023ed0b0cb485a6b62342934adb9e801956f5738b"

# RDEPENDS:${PN} += "python3-logging python3-threading"

# BBCLASSEXTEND = "native nativesdk"
