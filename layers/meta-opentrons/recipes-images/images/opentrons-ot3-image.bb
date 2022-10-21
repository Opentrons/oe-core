SUMMARY = "Opentrons OT3 Image"
DESCRIPTION = "Opentrons OT3 Robot Image"

LICENSE = "apache-2"

inherit core-image image_type_tezi

DEPENDS += "rsync-native zip-native"
IMAGE_FSTYPES += "ext4.xz teziimg"

IMAGE_LINGUAS = "en-us"
# Copy Licenses to image /usr/share/common-license
COPY_LIC_MANIFEST ?= "1"
COPY_LIC_DIRS ?= "1"

SYSTEMD_DEFAULT_TARGET = "graphical.target"

IMAGE_INSTALL += " \
    packagegroup-boot \
    packagegroup-basic \
    packagegroup-base-tdx-cli \
    packagegroup-tdx-cli \
    packagegroup-machine-tdx-cli \
    packagegroup-wifi-tdx-cli \
    packagegroup-wifi-fw-tdx-cli \
    packagegroup-tdx-graphical \
    packagegroup-fsl-isp \
    udev-extraconf \
    v4l-utils \
    bash coreutils makedevs mime-support util-linux \
    timestamp-service \
    networkmanager crda \
    ${@bb.utils.contains('DISTRO_FEATURES', 'systemd', 'timestamp-service systemd-analyze', '', d)} \
    weston-xwayland weston weston-init imx-gpu-viv \
    userfs-mount robot-app-wayland-launch robot-app \
    opentrons-robot-server opentrons-update-server \
    python3 python3-misc python3-modules \
    opentrons-usb-bridge \
 "

# We do NOT want the toradex libusbgx packages that autoconfigure the OTG USB
# port. Luckily, they are only recommended so it is easy to filter them out.
NO_RECOMMENDATIONS += " libusbgx libusbgx-examples"

# Prefix to the resulting deployable tarball name
export IMAGE_BASENAME = "opentrons-ot3-image"
MACHINE_NAME ?= "${MACHINE}"
IMAGE_NAME = "${MACHINE_NAME}_${IMAGE_BASENAME}"
SYSTEMFS_DIR = "${WORKDIR}/systemfs"
USERFS_DIR = "${WORKDIR}/userfs"
SYSTEMFS_OUTPUT = "${IMGDEPLOYDIR}/${IMAGE_LINK_NAME}.systemfs.ext4"
USERFS_OUTPUT = "${IMGDEPLOYDIR}/${IMAGE_LINK_NAME}.userfs.ext4"

# add the rootfs version to the welcome banner
add_rootfs_version() {
    printf "${DISTRO_NAME} ${DISTRO_VERSION} (${DISTRO_CODENAME}) \\\n \\\l\n" > ${IMAGE_ROOTFS}/etc/issue
    printf "${DISTRO_NAME} ${DISTRO_VERSION} (${DISTRO_CODENAME}) %%h\n" > ${IMAGE_ROOTFS}/etc/issue.net
    printf "${IMAGE_NAME}\n\n" >> ${IMAGE_ROOTFS}/etc/issue
    printf "${IMAGE_NAME}\n\n" >> ${IMAGE_ROOTFS}/etc/issue.net
}
ROOTFS_POSTPROCESS_COMMAND += "add_rootfs_version;"

fakeroot do_create_filesystem() {
    # this will create the systemfs tree
    rsync -aH --chown=root:root ${IMAGE_ROOTFS}/ ${SYSTEMFS_DIR} \
    --exclude='/home/*' --exclude '/var/*' --delete-excluded

    # create the userfs tree
    rsync -aH --chown=root:root ${IMAGE_ROOTFS}/home ${USERFS_DIR}/
    rsync -aH --chown=root:root ${IMAGE_ROOTFS}/var ${USERFS_DIR}/
    mkdir -p ${USERFS_DIR}/data

    # get size of the filesystem trees
    SYSTEMFS_SIZE=$(du -Lbks ${SYSTEMFS_DIR} | cut -f1)
    USERFS_SIZE=$(du -Lbks ${USERFS_DIR} | cut -f1)
 
    # create sparse file a bit larger than source dir
    dd if=/dev/zero of=${SYSTEMFS_OUTPUT} seek=${SYSTEMFS_SIZE}w bs=1024 count=0
    mkfs.ext4 -F ${SYSTEMFS_OUTPUT} -d ${SYSTEMFS_DIR}

    dd if=/dev/zero of=${USERFS_OUTPUT} seek=${USERFS_SIZE}b bs=1024 count=0
    mkfs.ext4 -F ${USERFS_OUTPUT} -d ${USERFS_DIR}

    # compress the systemfs.ext4
    xz -f -k -c -9 ${XZ_DEFAULTS} --check=crc32 ${SYSTEMFS_OUTPUT} > ${DEPLOY_DIR_IMAGE}/${IMAGE_LINK_NAME}.systemfs.ext4.xz

    # create the systemfs and userfs tarball
    tar --xattrs --xattrs-include=* --sort=name --format=posix --numeric-owner -cf ${DEPLOY_DIR_IMAGE}/${IMAGE_LINK_NAME}.systemfs.tar -C ${SYSTEMFS_DIR} ./
    tar --xattrs --xattrs-include=* --sort=name --format=posix --numeric-owner -cf ${DEPLOY_DIR_IMAGE}/${IMAGE_LINK_NAME}.userfs.tar -C ${USERFS_DIR} ./

    # compress the tarball
    xz -f -k -c -9 ${XZ_DEFAULTS} --check=crc32 ${DEPLOY_DIR_IMAGE}/${IMAGE_LINK_NAME}.systemfs.tar > ${DEPLOY_DIR_IMAGE}/${IMAGE_LINK_NAME}.systemfs.tar.xz
    xz -f -k -c -9 ${XZ_DEFAULTS} --check=crc32 ${DEPLOY_DIR_IMAGE}/${IMAGE_LINK_NAME}.userfs.tar > ${DEPLOY_DIR_IMAGE}/${IMAGE_LINK_NAME}.userfs.tar.xz
}

# create the tezi image.json
python do_create_tezi_manifest(){
    import os
    import json
    tezi_manifest_path = "%s/image-%s.json" % (d.getVar('DEPLOY_DIR_IMAGE'), d.getVar('IMAGE_BASENAME'))
    tezi_ot3_manifest_path = "%s/image.json" % (d.getVar('DEPLOY_DIR_IMAGE'))
    tezi_manifest = {}

    # define the ot3  partitions
    ot3_partitions = [{
                    "partition_size_nominal": 48,
                    "want_maximised": False,
                    "content": {
                        "label": "BOOT",
                        "filesystem_type": "FAT",
                        "mkfs_options": "",
                        "filename": "%s.bootfs.tar.xz" % (d.getVar('IMAGE_LINK_NAME')),
                        "uncompressed_size": 10.44921875
                    }
                },
                {
                    "partition_size_nominal": 1536,
                    "want_maximised": False,
                    "content": {
                        "label": "RFS",
                        "filesystem_type": "ext4",
                        "mkfs_options": "-E nodiscard",
                        "filename": "%s.systemfs.tar.xz" % (d.getVar('IMAGE_LINK_NAME')),
                    }
                },
                {
                    "partition_size_nominal": 1536,
                    "want_maximised": False,
                    "content": {
                        "label": "RFS2",
                        "filesystem_type": "ext4",
                        "mkfs_options": "-E nodiscard",
                        "filename": "%s.systemfs.tar.xz" % (d.getVar('IMAGE_LINK_NAME')),
                    }
                },
                {
                    "partition_size_nominal": 1024,
                    "want_maximised": True,
                    "content": {
                        "label": "DATA",
                        "filesystem_type": "ext4",
                        "filename": "%s.userfs.tar.xz" % (d.getVar('IMAGE_LINK_NAME')),
                    }
                }]

    if os.path.exists(tezi_manifest_path):
        with open(tezi_manifest_path, 'r') as fd:
            tezi_manifest = json.load(fd)
            for blockdev in tezi_manifest.get('blockdevs', []):
                if 'mmcblk0' in blockdev.get('name'):
                    blockdev['partitions'] = ot3_partitions
                    break;
        with open(tezi_ot3_manifest_path, 'w') as fd:
            json.dump(tezi_manifest, fd, indent=4)
    else:
        bb.error("Toradex manifest file not found - %s" % tezi_manifest_path)
        exit()
}

# create the opentrons ot3 manifest
python do_create_opentrons_manifest() {
    bb.note("Create the manifest json for for ot3-system.zip")
    import json
    import os

    opentrons_manifest = {}
    opentrons_json_output = "%s/VERSION.json" % d.getVar('DEPLOY_DIR_IMAGE')
    robot_server_version = "%s/opentrons-robot-server-version.json" % (d.getVar('DEPLOY_DIR_IMAGE'))
    update_server_version = "%s/opentrons-update-server-version.json" % (d.getVar('DEPLOY_DIR_IMAGE'))

    # grab the versions
    opentrons_versions = [robot_server_version, update_server_version]
    for version_path in opentrons_versions:
        if os.path.exists(robot_server_version):
            bb.note("opentrons %s exists!" % version_path)
            try:
                with open(version_path, 'r') as fh:
                    opentrons_manifest.update(json.load(fh))
            except JSONDecodeError:
                bb.error("Could not load opentrons version file - %s" % version_path)

    # create the VERSION.json file
    with open(opentrons_json_output, 'w') as fh:
        json.dump(opentrons_manifest, fh, indent=4)
}

# create the tezi ot3 image
fakeroot do_create_tezi_ot3() {
    tar --xattrs --xattrs-include=* --numeric-owner --transform \
    's,^,${TEZI_IMAGE_NAME}-Tezi_${TEZI_VERSION}/,' -chf  \
    ${DEPLOY_DIR_IMAGE}/${TEZI_IMAGE_NAME}-Tezi_${TEZI_VERSION}.tar -C \
    ${DEPLOY_DIR_IMAGE} toradexlinux.png marketing.tar prepare.sh wrapup.sh \
    LA_OPT_NXP_SW.html ${IMAGE_LINK_NAME}.systemfs.tar.xz ${IMAGE_LINK_NAME}.userfs.tar.xz \
    ${IMAGE_LINK_NAME}.bootfs.tar.xz u-boot-initial-env-sd imx-boot image.json
}

# create the opentrons ot3 image
do_create_opentrons_ot3() {
    cd ${DEPLOY_DIR_IMAGE}/

    # compute the sha256sum
    sha256sum ${IMAGE_LINK_NAME}.systemfs.ext4.xz > systemfs.xz.256

    # create the zip file
    zip ot3-system.zip ${IMAGE_LINK_NAME}.systemfs.ext4.xz systemfs.xz.256 \
    VERSION.json
}

do_create_filesystem[depends] += "virtual/fakeroot-native:do_populate_sysroot"
do_create_tezi_ot3[cleandirs] += "virtual/fakeroot-native:do_populate_sysroot"
do_create_tezi_ot3[dirs] += "${DEPLOY_DIR_IMAGE}"
do_create_tezi_manifest[dirs] += "${DEPLOY_DIR_IMAGE}"
do_create_tezi_manifest[prefuncs] += "do_image_teziimg"
do_create_tezi_ot3[prefuncs] += "do_image_teziimg do_create_filesystem"
do_create_opentrons_manifest[cleandirs] += "${DIPLOY_DIR_IMAGE}/opentrons-versions/"
do_create_opentrons_ot3[prefuncs] += "do_create_filesystem"
do_create_opentrons_ot3[dirs] += "${DIPLOY_DIR_IMAGE}"

addtask do_create_filesystem after do_image_complete before do_populate_lic_deploy
addtask do_create_tezi_manifest after do_create_filesystem before do_populate_lic_deploy
addtask do_create_tezi_ot3 after do_create_tezi_manifest before do_populate_lic_deploy
addtask do_create_opentrons_manifest after do_create_tezi_ot3 before do_populate_lic_deploy
addtask do_create_opentrons_ot3 after do_create_opentrons_manifest before do_populate_lic_deploy
