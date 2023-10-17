FILESEXTRAPATHS:prepend := "${THISDIR}/u-boot-toradex:"

SRC_URI:append = " \
     file://0001-verdin_imx8mm-remove-default-setup-args.patch \
"
