require ttf.inc

SUMMARY = "Adobe OpenType region specific font family for Simplified Chinese"
HOMEPAGE = "https://github.com/adobe-fonts/source-han-sans"
LICENSE = "OFL-1.1"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/${LICENSE};md5=fac3a519e5e9eb96316656e0ca4f2b90"

inherit allarch fontcache

SRC_URI = " \
    file://SourceHanSansCN-VF.ttf \
    file://44-source-han-sans-cn.conf \
"
SRC_URI[md5sum] = "80056a18882059d0f7d5616a929ca8a8"
SRC_URI[sha256sum] = "e7ed74bc82eddfb62bc9a09b7e850731ea40da8e8a1b530318c3fe395045ae6d"

do_install() {
    install -d ${D}${sysconfdir}/fonts/conf.d/
    install -m 0644 ${WORKDIR}/44-source-han-sans-cn.conf ${D}${sysconfdir}/fonts/conf.d/

    install -d ${D}${datadir}/fonts/
    install -m 0644 ${WORKDIR}/SourceHanSansCN-VF.ttf ${D}${datadir}/fonts/
}

FILES:${PN} = " \
    ${sysconfdir}/fonts \
    ${datadir}/fonts \
"
