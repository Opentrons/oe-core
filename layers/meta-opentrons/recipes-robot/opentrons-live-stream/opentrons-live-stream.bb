SUMMARY = "Service application for Opentrons FFMPEG Stream."
DESCRIPTION = "This will start the ffmpeg stream that delivers live video from a camera specified in the Opentrons FFMPEG Stream configuration."
LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/Apache-2.0;md5=89aea4e17d99a7cacdbeed46a0096b10"

inherit allarch systemd

S = "${WORKDIR}"

SRC_URI = " \
    file://opentrons-live-stream.service \
    file://opentrons-live-stream.sh \
    file://opentrons-live-stream.conf \
"

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

do_install () {
    install -d ${D}/${bindir} ${D}${systemd_unitdir}/system/
    install -m 0644 ${WORKDIR}/opentrons-live-stream.service ${D}${systemd_unitdir}/system
    install -m 0755 ${WORKDIR}/opentrons-live-stream.sh ${D}/${bindir}

    install -d ${D}/${datadir}
    install -d ${D}/${datadir}/opentrons
    install -m 0644 ${WORKDIR}/opentrons-live-stream.conf ${D}/${datadir}/opentrons/opentrons-live-stream.conf

FILES:${PN}:append := " \
    ${datadir}/opentrons/opentrons-live-stream.conf \
"

SYSTEMD_PACKAGES = "${PN}"
SYSTEMD_SERVICE:${PN} = "opentrons-live-stream.service"
