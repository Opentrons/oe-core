inherit externalsrc

EXTERNALSRC = "${@os.path.abspath(os.path.join("${TOPDIR}", os.pardir, os.pardir, "opentrons"))}"

LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://LICENSE;md5=3b83ef96387f14655fc854ddc3c6bd57"

inherit features_check

inherit insane

do_configure(){
    npm install -g yarn
    cd ${S}

    # Move the yarn package configs to a mapped location when running in container
    if [ ! -z "${YARN_CACHE_DIR}" ]; then
        bbnote "Seting the yarn cache location to - ${YARN_CACHE_DIR}"
        yarn config set cache-folder "${YARN_CACHE_DIR}"
        export electron_config_cache="${ELECTRON_CACHE_DIR}"
    fi

    yarn
    cd ${S}/app-shell-odd
    yarn electron-rebuild --arch=arm64
    cd ${S}
    # we removed setup-js from shared-data recently so let's allow it to fail so we
    # can handle both the is-there and the is-not-there case
    OPENTRONS_PROJECT=${OPENTRONS_PROJECT} make -C shared-data setup-js || true
}

do_compile(){
    cd ${S}
    export BUILD_ID=${CODEBUILD_BUILD_NUMBER:-dev}
    export NODE_OPTIONS=--openssl-legacy-provider
    export OPENSSL_MODULES=${STAGING_LIBDIR_NATIVE}/ossl-modules
    OT_APP_MIXPANEL_ID=${MIXPANEL_ID} OPENTRONS_PROJECT=${OPENTRONS_PROJECT} make -C ${S}/app dist
    OT_APP_MIXPANEL_ID=${MIXPANEL_ID} OPENTRONS_PROJECT=${OPENTRONS_PROJECT} make -C ${S}/app-shell-odd lib
    cd ${S}/app-shell-odd
    OT_APP_MIXPANEL_ID=${MIXPANEL_ID} OPENTRONS_PROJECT=${OPENTRONS_PROJECT} NODE_ENV=production NO_PYTHON=true yarn run electron-builder --config electron-builder.config.js --linux --arm64 --dir --publish never
}

fakeroot do_install(){
    DISTDIR=${S}/app-shell-odd/dist/linux-arm64-unpacked
    DESTDIR=${D}/opt/opentrons-app
    install -d ${D}/opt/opentrons-app
    cd ${DISTDIR}

    # This is needed to remove node_gyp_bins which contains symlinks outside the root causing failures in do_package_qa
    # @see https://github.com/nodejs/node-gyp/issues/2713
    # @see https://github.com/nodejs/node-gyp/pull/2721
    find -type d -name node_gyp_bins -prune -exec rm -rf "{}" \;

    find -type d -exec install -o root -g root -Dm 755 "{}" "${DESTDIR}/{}" \;
    find -type f -exec install -o root -g root -Dm 755 "{}" "${DESTDIR}/{}" \;
    # A side effect of using precompiled electron is that for some reason it
    # precompiles and bakes in some of its own versions of things that are
    # really system utilities and (in the case of wayland) actually can break
    # communication with the system because it uses a weird RPC thing and
    # really needs to get the system version. So we remove the local versions.
    # however, chrome for some reason opens these libraries via direct calls
    # or has a strict rpath or something so we need to symlink them explicitly
    rm ${DESTDIR}/libEGL.so ${DESTDIR}/libGLESv2.so ${DESTDIR}/libvulkan.so.1
    ln -s /usr/lib/libEGL.so ${DESTDIR}/libEGL.so
    ln -s /usr/lib/libGLESv2.so ${DESTDIR}/libGLESv2.so
    ln -s /usr/lib/libvulkan.so.1 ${DESTDIR}/libvulkan.so.1

}

REQUIRED_DISTRO_FEATURES = "x11"

do_install[depends] += "virtual/fakeroot-native:do_populate_sysroot"
INSANE_SKIP:${PN} = " already-stripped file-rdeps dev-so "
FILES:${PN} = "/opt/opentrons-app/* /opt/opentrons-app/**/*"
# todo figure out how to not need cups
RDEPENDS:${PN} = "udev \
                  nss \
                  dbus \
                  nspr libasound \
                  gtk+3 cairo \
                  libxcomposite libx11 libxrender libxext libx11-xcb libxi \
                  libxtst libxcursor libxrandr libxscrnsaver \
                  atk at-spi2-atk\
                  cups \
                  vulkan-loader vulkan-tools \
                  "
DEPENDS = " nodejs-native udev openssl-native "
