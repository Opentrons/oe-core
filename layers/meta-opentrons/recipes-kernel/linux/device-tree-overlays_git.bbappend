SRC_URI:append = "\
  file://verdin-imx8mm_sn65dsi84-atm0700l61_overlay.dts \
  file://verdin-imx8mm_gt911_overlay.dts \
  file://verdin-imx8mm_MCP2518_overlay.dts \
  file://verdin-imx8mm_usbotg1-force-peripheral.dts \
  file://verdin-imx8mm_force-lcd-on.dts \
  "

FILESEXTRAPATHS:append := ":${THISDIR}/overlays"

# this list of overlays will get included in the boot overlays dir
TEZI_EXTERNAL_KERNEL_DEVICETREE = "\
  verdin-imx8mm_sn65dsi84-atm0700l61_overlay.dtbo \
  verdin-imx8mm_gt911_overlay.dtbo \
  verdin-imx8mm_MCP2518_overlay.dtbo \
  verdin-imx8mm_force-lcd-on.dtbo \
  verdin-imx8mm_usbotg1-force-peripheral.dtbo \
"
# this list of overlays will get written to overlays.txt and
# applied at boot.

TEZI_EXTERNAL_KERNEL_DEVICETREE_BOOT = "\
  verdin-imx8mm_sn65dsi84-atm0700l61_overlay.dtbo \
  verdin-imx8mm_gt911_overlay.dtbo \
  verdin-imx8mm_MCP2518_overlay.dtbo \
  verdin-imx8mm_force-lcd-on.dtbo \
  verdin-imx8mm_usbotg1-force-peripheral.dtbo \
"


do_prep_opentrons_overlays () {
    cp ${WORKDIR}/*.dts ${WORKDIR}/git/overlays/
}

addtask prep_opentrons_overlays after do_patch before do_collect_overlays
