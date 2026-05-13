inherit externalsrc

EXTERNALSRC = "${@os.path.abspath(os.path.join("${TOPDIR}", os.pardir, os.pardir, "opentrons"))}"

LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://LICENSE;md5=3b83ef96387f14655fc854ddc3c6bd57"

inherit features_check

do_configure(){
    npm install -g pnpm@10

    # Monorepo chore_release-pd-8.10.2 declares packageManager=yarn; pnpm refuses any
    # command under ${S} (including pnpm config set). Set cache globally first, then strip
    # packageManager so pnpm install/exec match edge-style OE builds.
    if [ ! -z "${PNPM_CACHE_DIR}" ]; then
        bbnote "Setting global pnpm cache-folder to ${PNPM_CACHE_DIR}"
        ( cd / && pnpm config set cache-folder "${PNPM_CACHE_DIR}" --global )
        export electron_config_cache="${ELECTRON_CACHE_DIR}"
    fi

    cd ${S}
    python3 <<'PY'
import json
from pathlib import Path
p = Path("package.json")
if not p.is_file():
    raise SystemExit("package.json missing in monorepo root (externalsrc)")
data = json.loads(p.read_text(encoding="utf-8"))
if data.pop("packageManager", None) is not None:
    p.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")
PY

    export npm_config_package_manager_strict=false
    export CI=true
    pnpm install --config.package-manager-strict=false
    cd ${S}/js-package-testing
    make setup
    cd ${S}/app-shell-odd
    pnpm --config.package-manager-strict=false exec electron-rebuild --arch=arm64
    cd ${S}
    # we removed setup-js from shared-data recently so let's allow it to fail so we
    # can handle both the is-there and the is-not-there case
    OPENTRONS_PROJECT=${OPENTRONS_PROJECT} make -C shared-data setup-js || true
}

do_compile(){
    cd ${S}
    export npm_config_package_manager_strict=false
    export BUILD_ID=${CODEBUILD_BUILD_NUMBER:-dev}
    export NODE_OPTIONS=--openssl-legacy-provider
    export OPENSSL_MODULES=${STAGING_LIBDIR_NATIVE}/ossl-modules
    export CI=true
    
    OT_SENTRY_DSN="${OT_SENTRY_DSN}" \
    OT_SENTRY_AUTH_TOKEN="${OT_SENTRY_AUTH_TOKEN_OE_CORE}" \
    OT_APP_MIXPANEL_ID="${MIXPANEL_ID}" \
    OPENTRONS_PROJECT="${OPENTRONS_PROJECT}" \
    make -C ${S}/app dist

    OT_SENTRY_DSN="${OT_SENTRY_DSN}" \
    OT_SENTRY_AUTH_TOKEN="${OT_SENTRY_AUTH_TOKEN_OE_CORE}" \
    OT_APP_MIXPANEL_ID="${MIXPANEL_ID}" \
    OPENTRONS_PROJECT="${OPENTRONS_PROJECT}" \
    make -C ${S}/app-shell-odd lib

    # Remove incompatible Sentry CLI binaries that cause objcopy failures
    find . -name "sentry-cli" -type f -delete

    cd ${S}/app-shell-odd

    OT_BUILD_TARGET="${OT_BUILD_TARGET}" \
    OT_SENTRY_AUTH_TOKEN="${OT_SENTRY_AUTH_TOKEN_OE_CORE}" \
    OT_SENTRY_DSN="${OT_SENTRY_DSN}" \
    OT_APP_MIXPANEL_ID="${MIXPANEL_ID}" \
    OPENTRONS_PROJECT="${OPENTRONS_PROJECT}" \
    NODE_ENV=production \
    NO_PYTHON=true \
    pnpm --config.package-manager-strict=false exec electron-builder --config electron-builder.config.js --linux --arm64 --dir --publish never
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
