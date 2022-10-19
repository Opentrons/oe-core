SRC_URI_append = "\
  file://verdin-imx8mm_sn65dsi84-atm0700l61_overlay.dts \
  file://verdin-imx8mm_gt911_overlay.dts \
  file://verdin-imx8mm_MCP2518_overlay.dts \
  file://verdin-imx8mm_usbotg1-force-peripheral.dts \
  file://verdin-imx8mm_estop-force-on.dts \
  "

FILESEXTRAPATHS_append := ":${THISDIR}/overlays"

do_prep_opentrons_overlays () {
    cp ${WORKDIR}/*.dts ${WORKDIR}/git/overlays/
}

addtask prep_opentrons_overlays after do_patch before do_collect_overlays
