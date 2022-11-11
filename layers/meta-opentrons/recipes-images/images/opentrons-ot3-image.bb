SUMMARY = "Opentrons OT3 Image"
DESCRIPTION = "Opentrons OT3 Robot Image"

LICENSE = "apache-2"

inherit core-image image_type_tezi

DEPENDS += "rsync-native zip-native opentrons-robot-server opentrons-update-server"
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
 "

ROBOT_TYPE = "OT-3 Standard"
# Prefix to the resulting deployable tarball name
export IMAGE_BASENAME = "opentrons-ot3-image"
MACHINE_NAME ?= "${MACHINE}"
IMAGE_NAME = "${MACHINE_NAME}_${IMAGE_BASENAME}"
USERFS_DIR = "${WORKDIR}/userfs"
USERFS_OUTPUT = "${DEPLOY_DIR_IMAGE}/userfs.ext4"
# max rootfs partition size in mb
MAX_SYSTEMFS_SIZE = "1536"

# create the opentrons ot3 manifest (VERSION.json) file
python do_create_opentrons_manifest() {
    bb.note("Create the manifest json for for ot3-system.zip")
    import time
    import json
    import os

    opentrons_manifest = {
        'robot_type': d.getVar('ROBOT_TYPE'),
        'build_type': os.getenv('OT_BUILD_TYPE', 'unknown/dev'),
        'build_timestamp': time.time(),
        'openembedded_version': d.getVar('version', 'unknown'),
        'openembedded_sha': d.getVar('version', 'unknown'),
        'openembedded_branch': d.getVar('version', 'unknown')
    }

    # check that we have the expected version files and write them to the VERSION.json
    expected_opentrons_versions = ["opentrons-robot-server-version.json", \
                                   "opentrons-update-server-version.json"]

    opentrons_versions_dir = "%s/opentrons_versions" % d.getVar('STAGING_DIR_HOST')
    for version_file in os.listdir(opentrons_versions_dir):
        if version_file not in expected_opentrons_versions:
            bb.error("version file does not exist - %s" % version_file)
            exit()

        try:
           version_filepath = os.path.join(opentrons_versions_dir, version_file)
           with open(version_filepath, 'r') as fh:
                opentrons_manifest.update(json.load(fh))
        except (FileNotFoundError, json.JSONDecodeError):
            bb.error("Could not load opentrons version file - %s" % version_filepath)
            exit()

    # create the VERSION.json file
    opentrons_json_output = "%s/VERSION.json" % d.getVar('DEPLOY_DIR_IMAGE')
    with open(opentrons_json_output, 'w') as fh:
        json.dump(opentrons_manifest, fh, indent=4)
}
ROOTFS_PREPROCESS_COMMAND += "do_create_opentrons_manifest; "

# add the rootfs version to the welcome banner
fakeroot do_add_rootfs_version() {
    printf "${DISTRO_NAME} ${DISTRO_VERSION} (${DISTRO_CODENAME}) \\\n \\\l\n" > ${IMAGE_ROOTFS}/etc/issue
    printf "${DISTRO_NAME} ${DISTRO_VERSION} (${DISTRO_CODENAME}) %%h\n" > ${IMAGE_ROOTFS}/etc/issue.net
    printf "${IMAGE_NAME}\n\n" >> ${IMAGE_ROOTFS}/etc/issue
    printf "${IMAGE_NAME}\n\n" >> ${IMAGE_ROOTFS}/etc/issue.net

    # add datetime as version, example 202210111213
    date +"%Y%m%d%H%M" > ${IMAGE_ROOTFS}/etc/version

    # add the VERSION.json file
    cat ${DEPLOY_DIR_IMAGE}/VERSION.json > ${IMAGE_ROOTFS}/etc/VERSION.json

    # add hostname and machine-info
    printf "opentrons" > ${IMAGE_ROOTFS}/etc/hostname
    printf "PRETTY_HOSTNAME=opentrons\n" > ${IMAGE_ROOTFS}/etc/machine-info
    # TODO(ba, 2022-10-18): add proper mechanism for setting DEPLOYMENT
    printf "DEPLOYMENT=development\n" >> ${IMAGE_ROOTFS}/etc/machine-info
}
ROOTFS_POSTPROCESS_COMMAND += "do_add_rootfs_version; "

fakeroot do_create_filesystem() {
    # create the userfs tree
    rsync -aH --chown=root:root ${IMAGE_ROOTFS}/home ${USERFS_DIR}/
    rsync -aH --chown=root:root ${IMAGE_ROOTFS}/var ${USERFS_DIR}/
    mkdir -p ${USERFS_DIR}/data

    # create dir to persist network connections
    mkdir -p ${USERFS_DIR}/etc/NetworkManager/system-connections

    # cleanup dirs from rootfs
    rm -rf ${IMAGE_ROOTFS}/{home/*,var/*,unit_tests,opentrons_versions}

    # calculate size of the filesystem trees
    USERFS_SIZE=$(du -ks ${USERFS_DIR} | cut -f1)

    # add 3% to the actual size so mkfs has extra space
    USERFS_SIZE=`expr $USERFS_SIZE + $USERFS_SIZE \* 3 / 100`

    # create the userfs
    dd if=/dev/zero of=${USERFS_OUTPUT} seek=${USERFS_SIZE} count=60 bs=1024
    mkfs.ext4 -F ${USERFS_OUTPUT} -d ${USERFS_DIR}

    # create the userfs tarball
    tar --xattrs --xattrs-include=* --sort=name --format=posix --numeric-owner -cf ${DEPLOY_DIR_IMAGE}/${IMAGE_LINK_NAME}.userfs.tar -C ${USERFS_DIR} ./

    # compress the tarball
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
                    "partition_size_nominal": int(d.getVar("MAX_SYSTEMFS_SIZE")),
                    "want_maximised": False,
                    "content": {
                        "label": "RFS",
                        "filesystem_type": "ext4",
                        "mkfs_options": "-E nodiscard",
                        "filename": "%s.tar.xz" % (d.getVar('IMAGE_LINK_NAME')),
                    }
                },
                {
                    "partition_size_nominal": int(d.getVar('MAX_SYSTEMFS_SIZE')),
                    "want_maximised": False,
                    "content": {
                        "label": "RFS2",
                        "filesystem_type": "ext4",
                        "mkfs_options": "-E nodiscard",
                        "filename": "%s.tar.xz" % (d.getVar('IMAGE_LINK_NAME')),
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

            # enable image autoinstall
            tezi_manifest['autoinstall'] = True

            # setup the partitions
            for blockdev in tezi_manifest.get('blockdevs', []):
                if 'mmcblk0' in blockdev.get('name'):
                    blockdev['partitions'] = ot3_partitions
                    break;
        with open(tezi_ot3_manifest_path, 'w') as fd:
            json.dump(tezi_manifest, fd, indent=4)
    else:
        bb.error("Toradex manifest file not found - %s" % tezi_manifest_path)
        exit(1)
}

# create the tezi ot3 image
fakeroot do_create_tezi_ot3() {
    tar --xattrs --xattrs-include=* --numeric-owner --transform \
    's,^,${TEZI_IMAGE_NAME}-Tezi_${TEZI_VERSION}/,' -chf  \
    ${DEPLOY_DIR_IMAGE}/${TEZI_IMAGE_NAME}-Tezi_${TEZI_VERSION}.tar -C \
    ${DEPLOY_DIR_IMAGE} toradexlinux.png marketing.tar prepare.sh wrapup.sh \
    LA_OPT_NXP_SW.html ${IMAGE_LINK_NAME}.tar.xz ${IMAGE_LINK_NAME}.userfs.tar.xz \
    ${IMAGE_LINK_NAME}.bootfs.tar.xz u-boot-initial-env-sd imx-boot image.json
}

# create the opentrons ot3 image
do_create_opentrons_ot3() {
    cd ${DEPLOY_DIR_IMAGE}/
    cp opentrons-ot3-image-verdin-imx8mm.ext4.xz systemfs.xz

    # compute the sha256sum
    sha256sum systemfs.xz | cut -d " " -f 1 > systemfs.xz.sha256

    # create the zip file
    zip ot3-system.zip systemfs.xz systemfs.xz.sha256 VERSION.json
}

do_create_filesystem[depends] += "virtual/fakeroot-native:do_populate_sysroot"
do_create_tezi_manifest[prefuncs] += "do_image_teziimg"

do_create_tezi_ot3[depends] += "virtual/fakeroot-native:do_populate_sysroot"
do_create_tezi_ot3[prefuncs] += "do_image_teziimg do_create_filesystem"

addtask do_create_filesystem after do_image_complete before do_populate_lic_deploy
addtask do_create_tezi_manifest after do_create_filesystem before do_populate_lic_deploy
addtask do_create_tezi_ot3 after do_create_tezi_manifest before do_populate_lic_deploy
addtask do_create_opentrons_ot3 after do_create_tezi_ot3 before do_populate_lic_deploy
