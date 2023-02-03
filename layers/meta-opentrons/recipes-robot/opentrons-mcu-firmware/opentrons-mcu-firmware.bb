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
    cmake --build --preset=all-application-firmware
    # get the submodule versions to be used when creating the opentrons-firmware.json file
    python3 ${S}/scripts/subsystem_versions.py --output ${S}/subsystem_versions.json
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

    # pull in the subsystem_version.json file that was created during compilation and create the opentrons-firmware.json file
    subsystems = {}
    filepath = "%s/subsystem_versions.json" % d.getVar('S')
    try:
        with open(filepath, 'r') as fh:
            subsystems = json.load(fh)
    except subprocess.CalledProcessError as cpe:
        bb.error(f"Could not load submodule_versions.json file {filepath} {cpe}.")
        exit()

    manifest = {
        "manifest_version": 1,
        "subsystems": subsystems
    }

    # add the filepath
    firmware_path = d.getVar("FIRMWARE_DIR")
    for subsystem, update_info in manifest['subsystems'].items():
        for rev, filename in update_info.get('files_by_revision').items():
            manifest['subsystems'][subsystem]['files_by_revision'].update({
                rev: f"{firmware_path}/{filename}"
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
