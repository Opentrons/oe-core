DESCRIPTION = "builds and installs the update binaries for the ot3 firmware."
LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://LICENSE;md5=86d3f3a95c324c9479bd8986968f4327"

inherit externalsrc pkgconfig cmake

FIRMWARE_DIR="${libdir}/firmware"
S = "${WORKDIR}"
B = "${S}/build"

EXTERNALSRC = "${@os.path.abspath(os.path.join("${TOPDIR}", os.pardir, os.pardir, "ot3-firmware"))}"
DEPENDS += " cmake-native"

do_configure(){
    cd ${S}/
    cmake --preset=cross .
}

do_compile(){
    cd ${S}/
    cmake --build --preset=all
}

do_install(){
    # install the compiled binaries to /usr/lib/firmware
    install -d ${D}${FIRMWARE_DIR}
    find ${S} -type f -name "*.hex" -exec install -m 0644 {} ${D}${FIRMWARE_DIR} \;

    # install the manifest file
    install -m 644 ${S}/opentrons-firmware.json ${D}/${FIRMWARE_DIR}/opentrons-firmware.json
}

python do_create_manifest(){
    # This will create a manifest.json file which will describe our firmware updates
    import json
    import subprocess

    # Get the version, sha, and branch
    try:
        version = subprocess.check_output(['git', 'describe', '--tags', '--always']).decode().strip()
        commit_sha = subprocess.check_output(['git', 'rev-parse', 'HEAD']).decode().strip()
        branch = subprocess.check_output(['git', 'rev-parse', '--abbrev-ref', 'HEAD']).decode().strip()
    except subprocess.CalledProcessError as cpe:
        bb.error("Could not get oe-core version - %s" % cpe)
        exit()

    manifest = {
        "manifest_version": 1,
        "subsystems": {
            "head": {},
            "gantry-x": {},
            "gantry-y": {},
            "gripper": {},
            "pipettes-single": {},
            "pipettes-multi": {},
            "pipettes-96": {},
            "pipettes-384": {}
        }
    }

    # add the subsystem version and filepath
    for subsystem in manifest['subsystems']:
        filepath = "%s/%s.hex" % (d.getVar("FIRMWARE_DIR"), subsystem)
        manifest['subsystems'][subsystem].update({
            "commit_sha": commit_sha,
            "branch": branch,
            "version": version,
            "filepath": filepath
        })

    # save manifest file to disk
    manifest_file = "%s/opentrons-firmware.json" % (d.getVar("S"))
    with open(manifest_file, "w") as fh:
        json.dump(manifest, fh)
}

# since we are compiling binaries for the subsystem which has a different arch to linux we need
# to ignore the architecture.
INSANE_SKIP_${PN} += "arch"

FILES_${PN} += "${libdir}/firmware \
                ${libdir}/firmware/head-rev1.hex \
                ${libdir}/firmware/gantry-x-rev1.hex \
                ${libdir}/firmware/gantry-y-rev1.hex \
                ${libdir}/firmware/gripper-rev1.hex \
                ${libdir}/firmware/pipettes-single-rev1.hex \
                ${libdir}/firmware/pipettes-multi-rev1.hex \
                ${libdir}/firmware/pipettes-96-rev1.hex \
                ${libdir}/firmware/opentrons-firmware.json"

addtask do_create_manifest after do_compile before do_install
