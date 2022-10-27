LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://LICENSE;md5=3b83ef96387f14655fc854ddc3c6bd57"
SRC_URI = "git://github.com/Opentrons/opentrons.git;protocol=https;branch=edge; "

# Modify these as desired
PV = "1.0+git${SRCPV}"

inherit features_check

SRCREV = "${AUTOREV}"
S = "${WORKDIR}/git"

inherit insane


do_configure(){
    npm install -g yarn
    cd ${S}
    yarn
    cd ${S}/app-shell
    yarn electron-rebuild --arch=arm64
    cd ${S}
    make -C shared-data setup-js
}

do_compile(){
    export BUILD_ID=${CODEBUILD_BUILD_NUMBER:-dev}
    make -C app dist
    make -C app-shell lib
    cd ${S}/app-shell
    NODE_ENV=production NO_PYTHON=true yarn run electron-builder --config electron-builder.config.js --linux --arm64 --dir --publish never
}

fakeroot do_install(){
    DISTDIR=${S}/app-shell/dist/linux-arm64-unpacked
    DESTDIR=${D}/opt/opentrons-app
    install -d ${D}/opt/opentrons-app
    cd ${DISTDIR}
    find -type d -exec install -o root -g root -Dm 755 "{}" "${DESTDIR}/{}" \;
    find -type f -exec install -o root -g root -Dm 755 "{}" "${DESTDIR}/{}" \;
    # A side effect of using precompiled electron is that for some reason it
    # precompiles and bakes in some of its own versions of things that are
    # really system utilities and (in the case of wayland) actually can break
    # communication with the system because it uses a weird RPC thing and
    # really needs to get the system version. So we remove the local versions.
    rm ${DESTDIR}/libEGL.so ${DESTDIR}/libGLESv2.so ${DESTDIR}/libvulkan.so.1

    # This is needed to remove node_gyp_bins which contains symlinks outside the root
    # @see https://github.com/nodejs/node-gyp/issues/2713
    # @see https://github.com/nodejs/node-gyp/pull/2721
    #cd ${DESTDIR}
    bbnote "${DESTDIR} something"
    find -type d -name node_gyp_bins -exec rm -rf "{}" \;
}

REQUIRED_DISTRO_FEATURES = "x11"

do_install[depends] += "virtual/fakeroot-native:do_populate_sysroot"
INSANE_SKIP_${PN} = " already-stripped file-rdeps"
FILES_${PN} = "/opt/opentrons-app/* /opt/opentrons-app/**/*"
# todo figure out how to not need cups
RDEPENDS_${PN} = "udev \
                  nss \
                  dbus \
                  nspr libasound \
                  gtk+3 cairo \
                  libxcomposite libx11 libxrender libxext libx11-xcb libxi \
                  libxtst libxcursor libxrandr libxscrnsaver \
                  atk at-spi2-atk\
                  cups"
DEPENDS = " nodejs-native udev"
