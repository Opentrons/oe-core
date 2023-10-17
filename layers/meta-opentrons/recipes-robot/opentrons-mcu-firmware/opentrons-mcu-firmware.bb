DESCRIPTION = "builds and installs the update binaries for the ot3 firmware."
LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://LICENSE;md5=86d3f3a95c324c9479bd8986968f4327"

inherit externalsrc pkgconfig cmake

FIRMWARE_DIR="${libdir}/firmware"

EXTERNALSRC = "${@os.path.abspath(os.path.join("${TOPDIR}", os.pardir, os.pardir, "ot3-firmware"))}"
DEPENDS += " cmake-native"

do_configure(){
    cd ${S}/
    cmake --preset=cross-no-directory-reqs -B ${B}/build-cross --install-prefix=${B}/dist .

}

do_compile(){
    cd ${S}/

    cmake --build ${B}/build-cross --target firmware-applications
    cmake --install ${B}/build-cross --component Applications
}

python do_create_version_include() {
    bb.note("Create the version file entries for firmware")
    import subprocess
    import json
    import os
    try:
        fw_version = subprocess.check_output(
            ['git', 'describe', '--tags', '--always'],
            cwd=d.getVar("S")).decode().strip()
        fw_sha = subprocess.check_output(
            ['git', 'rev-parse', 'HEAD'],
            cwd=d.getVar("S")).decode().strip()
        fw_branch=subprocess.check_output(
            ['git', 'rev-parse', '--abbrev-ref', 'HEAD'],
            cwd=d.getVar("S")).decode().strip()
    except subprocess.CalledProcessError as cpe:
        bb.error("Could not get ot3-firmware version - %s" % cpe)
        exit()

    fw_manifest_chunk = {'firmware_version': fw_version,
                         'firmware_sha': fw_sha,
                         'firmware_branch': fw_branch}
    version_filepath = d.getVar('B') + '/firmware-versions.json'
    with open(version_filepath, 'w') as vf:
        json.dump(fw_manifest_chunk, vf)
}

addtask do_create_version_include after do_compile before do_install

do_install(){
    # install the compiled binaries to /usr/lib/firmware
    install -d ${D}${FIRMWARE_DIR}
    find ${B}/dist/applications -type f -exec install -m 0644 {} ${D}${FIRMWARE_DIR} \;
    install -d ${D}/opentrons_versions
    cat ${B}/firmware-versions.json > ${D}/opentrons_versions/opentrons-firmware-version.json
}

# since we are compiling binaries for the subsystem which has a different arch to linux we need
# to ignore the architecture.
INSANE_SKIP:${PN} += "arch"

SYSROOT_DIRS += "\
                /opentrons_versions\
                "

FILES:${PN} += "${libdir}/firmware/head-*.hex \
                ${libdir}/firmware/gantry-*.hex \
                ${libdir}/firmware/gripper-*.hex \
                ${libdir}/firmware/pipettes-*.hex \
                ${libdir}/firmware/rear-panel-*.bin \
                ${libdir}/firmware/opentrons-firmware.json \
                /opentrons_versions/opentrons-firmware-version.json \
                "
