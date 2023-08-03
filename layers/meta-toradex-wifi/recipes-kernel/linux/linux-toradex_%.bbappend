FILESEXTRAPATHS_prepend_mfg-mode := "${THISDIR}/files:"

RDEPENDS_${PN} += "toradex-wifi-config"

SRC_URI_append_mfg-mode = " file://0001-wext-Support-old-style-ioctls-for-NXP-Azurewave-Marv.patch "
