SUMMARY = "Opentrons OT3 Image"
DESCRIPTION = "Opentrons OT3 Robot Image"

LICENSE = "apache-2"

inherit core-image image_type_tezi

DEPENDS += "rsync-native zip-native \
    opentrons-robot-server \
    opentrons-update-server \
    opentrons-usb-bridge \
    opentrons-system-server \
    opentrons-mcu-firmware \
    "
IMAGE_FSTYPES += "ext4.xz teziimg"

IMAGE_LINGUAS = "en-us"
# Copy Licenses to image /usr/share/common-license
COPY_LIC_MANIFEST ?= "1"
COPY_LIC_DIRS ?= "1"

SYSTEMD_DEFAULT_TARGET = "graphical.target"

# TODO(BA, 1-11-23): We are ignoring the build-type for now and always building with debug-tweaks
# enabled. This is because ommiting debug-tweaks disables some development friendly features
# (no root pw, ssh to root, etc) which for now we want to keep. Once we add non-root users
# and fixed known root passwords we can enable this.
#EXTRA_IMAGE_FEATURES += " \
#    ${@bb.utils.contains('OT_BUILD_TYPE', 'develop', 'debug-tweaks', '', d)} \
#"
EXTRA_IMAGE_FEATURES += " debug-tweaks"

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
    v4l-utils dfu-util \
    bash coreutils makedevs mime-support util-linux \
    timestamp-service networkmanager crda ch341ser \
    ${@bb.utils.contains('DISTRO_FEATURES', 'systemd', 'timestamp-service systemd-analyze', '', d)} \
    weston-xwayland weston weston-init imx-gpu-viv \
    robot-app-wayland-launch opentrons-robot-app \
    opentrons-robot-server opentrons-update-server \
    python3 python3-misc python3-modules python3-jupyter \
    opentrons-jupyter-notebook opentrons-usb-bridge \
    opentrons-system-server opentrons-mcu-firmware \
    opentrons-user-environment opentrons-module-firmware \
    opentrons-systemd-units \
 "

# We do NOT want the toradex libusbgx packages that autoconfigure the OTG USB
# port. Luckily, they are only recommended so it is easy to filter them out.
PACKAGE_EXCLUDE = "libusbgx libusbgx-examples"

# exclude Toradex hostapd-example as this causes mDNS discovery issues when interface uap0 connects/disconnects.
PACKAGE_EXCLUDE += " hostapd-example"

ROBOT_TYPE = "OT-3 Standard"

# Prefix to the resulting deployable tarball name
export IMAGE_BASENAME = "opentrons-ot3-image"
MACHINE_NAME ?= "${MACHINE}"
IMAGE_NAME = "${MACHINE_NAME}_${IMAGE_BASENAME}"
USERFS_DIR = "${WORKDIR}/userfs"
USERFS_OUTPUT = "${DEPLOY_DIR_IMAGE}/userfs.ext4"
# max rootfs partition size in mb
MAX_SYSTEMFS_SIZE = "2048"

# create the opentrons ot3 manifest (VERSION.json) file
python do_create_opentrons_manifest() {
    bb.note("Create the manifest json for for ot3-system.zip")
    import subprocess
    import json
    import os

    # Get the oe-core version, sha, branch
    try:
        oe_version = subprocess.check_output(['git', 'describe', '--tags', '--always']).decode().strip()
        oe_sha = subprocess.check_output(['git', 'rev-parse', 'HEAD']).decode().strip()
        oe_branch = subprocess.check_output(['git', 'rev-parse', '--abbrev-ref', 'HEAD']).decode().strip()
    except subprocess.CalledProcessError as cpe:
        bb.error("Could not get oe-core version - %s" % cpe)
        exit()

    # Create the manifest dictionary
    opentrons_manifest = {
        'robot_type': d.getVar('ROBOT_TYPE'),
        'build_type': d.getVar('OT_BUILD_TYPE', 'develop'),
        'openembedded_version': oe_version,
        'openembedded_sha': oe_sha,
        'openembedded_branch': oe_branch
    }

    # check that we have the expected version files and write them to the VERSION.json
    expected_opentrons_versions = ["opentrons-robot-server-version.json", \
                                   "opentrons-update-server-version.json", \
                                   "opentrons-system-server-version.json", \
                                   "opentrons-api-version.json", \
                                   "opentrons-usb-bridge-version.json", \
                                   "opentrons-firmware-version.json"]

    opentrons_versions_dir = "%s/opentrons_versions" % d.getVar('STAGING_DIR_HOST')
    version_files_present = os.listdir(opentrons_versions_dir)
    for version_file in expected_opentrons_versions:
        if version_file not in version_files_present:
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

# changes we might want to make to the rootfs
do_make_rootfs_changes() {
    printf "${DISTRO_NAME} ${DISTRO_VERSION} (${DISTRO_CODENAME}) \\\n \\\l\n" > ${IMAGE_ROOTFS}${sysconfdir}/issue
    printf "${DISTRO_NAME} ${DISTRO_VERSION} (${DISTRO_CODENAME}) %%h\n" > ${IMAGE_ROOTFS}${sysconfdir}/issue.net
    printf "${IMAGE_NAME}\n\n" >> ${IMAGE_ROOTFS}${sysconfdir}/issue
    printf "${IMAGE_NAME}\n\n" >> ${IMAGE_ROOTFS}${sysconfdir}/issue.net

    # add the VERSION.json file
    cat ${DEPLOY_DIR_IMAGE}/VERSION.json > ${IMAGE_ROOTFS}${sysconfdir}/VERSION.json
    # copy the release notes to the output dir
    cat ${IMAGE_ROOTFS}${sysconfdir}/release-notes.md > ${DEPLOY_DIR_IMAGE}/release-notes.md

    # add hostname to rootfs
    printf "opentrons" > ${IMAGE_ROOTFS}${sysconfdir}/hostname
    printf "PRETTY_HOSTNAME=opentrons\n" > ${IMAGE_ROOTFS}${sysconfdir}/machine-info
    printf "DEPLOYMENT=development\n" >> ${IMAGE_ROOTFS}${sysconfdir}/machine-info

    # copy the boot files to the /boot dir
    rsync -aL --chown=root:root  ${DEPLOY_DIR_IMAGE}/Image.gz ${IMAGE_ROOTFS}/boot/
    rsync -aL --chown=root:root  ${DEPLOY_DIR_IMAGE}/boot.scr* ${IMAGE_ROOTFS}/boot/boot.scr
    rsync -aL --chown=root:root  ${DEPLOY_DIR_IMAGE}/overlays* ${IMAGE_ROOTFS}/boot/
    rsync -aL --chown=root:root  ${DEPLOY_DIR_IMAGE}/imx8mm-verdin*dev.dtb ${IMAGE_ROOTFS}/boot/
    rsync -aL --chown=root:root  ${DEPLOY_DIR_IMAGE}/imx8mm-verdin*dahlia.dtb ${IMAGE_ROOTFS}/boot/

    # cleanup
    rm -rf ${IMAGE_ROOTFS}/opentrons_versions
}
ROOTFS_POSTPROCESS_COMMAND += "do_make_rootfs_changes; "

fakeroot do_create_filesystem() {
    # check that the size of the rootfs is not greater than the max systemfs partition
    systemfs=${DEPLOY_DIR_IMAGE}/${IMAGE_LINK_NAME}.ext4.xz
    systemfs_size_raw=$(xz -l ${systemfs} | awk 'FNR == 2 {print $5}' | tr -d ',')
    systemfs_size_mb=${systemfs_size_raw%.*}
    rootfs_overflow=`expr ${systemfs_size_mb} \> ${MAX_SYSTEMFS_SIZE}` || echo 0
    if [ ${rootfs_overflow} = 1 ]; then
        bberror "CRITICAL: Rootfs size ${systemfs_size_mb} is greater than max allowed ${MAX_SYSTEMFS_SIZE}!."
        exit 1
    fi

    # create the userfs tree
    rsync -aH --chown=root:root ${IMAGE_ROOTFS}/home ${USERFS_DIR}/
    rsync -aH --chown=root:root ${IMAGE_ROOTFS}/var ${USERFS_DIR}/
    mkdir -p ${USERFS_DIR}/data
    mkdir -p ${USERFS_DIR}${sysconfdir}
    rm -rf ${USERFS_DIR}/var/log
    mkdir -p ${USERFS_DIR}/var/log

    # add hostname and machine-info to userfs
    cat ${IMAGE_ROOTFS}${sysconfdir}/hostname > ${USERFS_DIR}${sysconfdir}/hostname
    cat ${IMAGE_ROOTFS}${sysconfdir}/machine-info > ${USERFS_DIR}${sysconfdir}/machine-info

    # cleanup dirs from rootfs
    rm -rf ${IMAGE_ROOTFS}/home/*
    rm -rf ${IMAGE_ROOTFS}/var/*

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
    ln -f opentrons-ot3-image-verdin-imx8mm.ext4.xz systemfs.xz

    # compute the sha256sum
    sha256sum systemfs.xz | cut -d " " -f 1 > systemfs.xz.sha256

    # sign the hash
    signed_rootfs=""
    bberror "TRY AND CREATE SIGNED BUILD"
    if [ -e "${SIGNING_KEY}" ]; then
        bberror "Signing the build"
        openssl dgst -sha256 -sign "${SIGNING_KEY}" -out systemfs.xz.sha256.sig systemfs.xz.sha256
    fi

    # create the zip file
    zip ot3-system.zip systemfs.xz systemfs.xz.sha256 $signed_rootfs VERSION.json
}

do_create_filesystem[depends] += "virtual/fakeroot-native:do_populate_sysroot"
do_create_tezi_manifest[prefuncs] += "do_image_teziimg"

do_create_tezi_ot3[depends] += "virtual/fakeroot-native:do_populate_sysroot"
do_create_tezi_ot3[prefuncs] += "do_image_teziimg do_create_filesystem"

addtask do_create_filesystem after do_image_complete before do_populate_lic_deploy
addtask do_create_tezi_manifest after do_create_filesystem before do_populate_lic_deploy
addtask do_create_tezi_ot3 after do_create_tezi_manifest before do_populate_lic_deploy
addtask do_create_opentrons_ot3 after do_create_tezi_ot3 before do_populate_lic_deploy
