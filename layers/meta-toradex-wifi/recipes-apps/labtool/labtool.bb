SUMMARY = "Labtool application for AzureWave manufacturing mode tests."
LICENSE = "CLOSED"

SRC_URI = " \
    ${NXP_PROPRIETARY_DRIVER_LOCATION}/${NXP_PROPRIETARY_MFG_TOOL_FILENAME};subdir=archive \
    file://0001-Adapt-makefile-for-yocto-build.patch \
    file://0002-Bypass-problems-with-redefinition-of-min-and-max-std.patch \
    file://0003-Remove-strip-from-the-build.patch \
"
ROOT_HOME="/home/root"

TARGET_CC_ARCH += "${LDFLAGS}"

FILES_${PN} = "${ROOT_HOME}/labtool ${ROOT_HOME}/SetUp.ini"

addtask labtool_sanity_check before do_fetch
python do_labtool_sanity_check() {
    if ("mfg-mode" not in d.getVar('OVERRIDES').split(":")):
        bb.fatal("Building the labtool recipe requires mfg-mode.")
}

addtask nxp_driver_unpack before do_patch after do_unpack
do_nxp_driver_unpack() {
    DIRNAME=$(echo ${NXP_PROPRIETARY_MFG_TOOL_FILENAME} | sed 's/\.zip//')
    DRVNAME=$(basename ${NXP_PROPRIETARY_MFG_TOOL_FILENAME} | sed 's/zip/tar/')
    tar -C ${S} \
        --strip-components=2 \
        -xf ${WORKDIR}/archive/${DIRNAME}/Labtool/labtool_1.1.0.188.0-src.tgz
}

do_compile() {
    oe_runmake -f MakeFile_W8997_FC18 -k -C DutApiWiFiBt
}

do_install() {
    install -d ${D}${ROOT_HOME}
    install -m 0644 ${B}/DutApiWiFiBt/SetUp.ini ${D}${ROOT_HOME}
    install -m 0755 ${B}/DutApiWiFiBt/labtool ${D}${ROOT_HOME}
}

DEPENDS += "bluez5"
