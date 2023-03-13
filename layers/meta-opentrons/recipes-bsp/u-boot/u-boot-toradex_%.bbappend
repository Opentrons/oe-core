FILESEXTRAPATHS_prepend := "${THISDIR}/u-boot-toradex:"

SRC_URI_append = " \
     file://0001-verdin_imx8mm-remove-default-setup-args.patch \
"
