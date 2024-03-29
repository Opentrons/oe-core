SUMMARY = "Wayland application autostart for Opentrons App"
DESCRIPTION = "This will start the robot app after the wayland socket has been created."
LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/Apache-2.0;md5=89aea4e17d99a7cacdbeed46a0096b10"

inherit allarch systemd

RDEPENDS:${PN}:append = "weston-init opentrons-robot-app systemd-extra-utils gstd"

S = "${WORKDIR}"

SRC_URI = " \
    file://opentrons-robot-app.service.in \
    file://opentrons-robot-app.sh.in \
    file://setup-tps65154.sh \
    file://configure-screen-power.service \
    file://opentrons-robot-app-devtools.service \
    file://opentrons-robot-app-devtools.socket \
    file://opentrons-loading.service \
    file://opentrons-loading.sh \
    file://loading.mp4 \
"

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

APPLICATION_ENVIRONMENT := '\"WAYLAND_DISPLAY=wayland-0\" \"XDG_RUNTIME_DIR=/run/user/0\"  \"DISPLAY=\:0\:0\" \"XDG_SESSION_TYPE=wayland\" \"XDG_SESSION_DESKTOP=kiosk\" \"PYTHONPATH=/opt/opentrons-robot-server\"'

WAYLAND_APPLICATION := "/opt/opentrons-app/opentrons \
    --disable-gpu \
    --remote-allow-origin=* \
    --remote-debugging-port=9222 \
    --discovery.candidates=localhost \
    --discovery.ipFilter=\"127.0.0.1\" \
    --isOnDevice=1 \
    --no-sandbox \
    --enable-features=UseOzonePlatform \
    --ozone-platform=wayland \
    --in-process-gpu \
    --python.pathToPythonOverride=/usr/bin/python3\
    "

do_compile () {
    sed -e "s:@@wayland-application@@:${WAYLAND_APPLICATION}:" -e "s:@@initial-path@@:${INITIAL_PATH}:" opentrons-robot-app.sh.in > opentrons-robot-app.sh
    sed -e "s:@@application-environment@@:${APPLICATION_ENVIRONMENT}:" opentrons-robot-app.service.in > opentrons-robot-app.service
}

do_install () {
    install -d ${D}/${bindir} ${D}${systemd_unitdir}/system/
    install -m 0644 ${WORKDIR}/opentrons-robot-app.service ${D}${systemd_unitdir}/system
    install -m 0755 ${S}/opentrons-robot-app.sh ${D}/${bindir}

    install -m 0644 ${WORKDIR}/configure-screen-power.service ${D}${systemd_unitdir}/system
    install -m 0755 ${WORKDIR}/setup-tps65154.sh ${D}/${bindir}

    install -m 0644 ${WORKDIR}/opentrons-robot-app-devtools.socket ${D}${systemd_unitdir}/system/
    install -m 0644 ${WORKDIR}/opentrons-robot-app-devtools.service ${D}${systemd_unitdir}/system/

    install -m 0755 ${S}/opentrons-loading.sh ${D}/${bindir}
    install -m 0644 ${WORKDIR}/opentrons-loading.service ${D}${systemd_unitdir}/system
    install -d ${D}/${datadir}
    install -d ${D}/${datadir}/opentrons
    install -m 0644 ${S}/loading.mp4 ${D}/${datadir}/opentrons/loading.mp4
}

FILES:${PN}:append := " ${datadir}/opentrons/loading.mp4 "


SYSTEMD_PACKAGES = "${PN}"
SYSTEMD_SERVICE:${PN} = "opentrons-robot-app.service configure-screen-power.service opentrons-loading.service opentrons-robot-app-devtools.service opentrons-robot-app-devtools.socket"
