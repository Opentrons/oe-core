#!/bin/bash

echo 'BBLAYERS += " ${TOPDIR}/../layers/meta-toradex-wifi"' >> build/conf/bblayers.conf
echo 'INHERIT += "toradex-wifi-nxp-proprietary-driver"' >> build/conf/auto.conf
echo 'NXP_PROPRIETARY_DRIVER_LOCATION = "file:///${TOPDIR}/wifi-archive"' >> build/conf/auto.conf

cat << EOF >> build/conf/auto.conf
NXP_PROPRIETARY_DRIVER_FILENAME_interface-diversity-pcie-usb = "PCIE-WLAN-USB-BT-8997-U16-X86-W16.88.10.p173-16.26.10.p173-C4X16698_V4-MGPL.zip"
NXP_PROPRIETARY_DRIVER_SHA256_interface-diversity-pcie-usb="24edfcc985c9c7710c7a8f96de8e8b5ed3037c76126c48a49486f4e517ab5335"
NXP_PROPRIETARY_DRIVER_FILENAME_interface-diversity-sd-sd= "SD-WLAN-SD-BT-8997-U16-MMC-W16.68.10.p162-16.26.10.p162-C4X16693_V4-MGPL.zip"
NXP_PROPRIETARY_DRIVER_SHA256_interface-diversity-sd-sd="37426f9e57974d064ad8edd37adbd3b7fbeb8efd5fb7471cc35a919de51b0d15"
NXP_PROPRIETARY_DRIVER_FILENAME_interface-diversity-sd-uart= "SD-WLAN-UART-BT-8997-LNX_5_15_71-IMX8-16.92.21.p55.3-16.92.21.p55.3-MM5X16366.P5-MGPL.zip"
NXP_PROPRIETARY_DRIVER_SHA256_interface-diversity-sd-uart="2b2f4557dfb5b793c74141a4ed32ae75c9705225c58f1cf15d7947878a9fd66b"
NXP_PROPRIETARY_MFG_TOOL_FILENAME="MFG-W8997-MF-LABTOOL-ANDROID-1.1.0.188.0-16.80.205.p208.zip"
NXP_PROPRIETARY_MFG_TOOL_SHA256="599031b9040c3a501f656a30f85308b9a1929ed5d1f7c40f14c370298f8ba8f9"
EOF

echo 'MACHINEOVERRIDES =. "default-nxp-proprietary-driver:"' >> build/conf/local.conf
echo '#MACHINEOVERRIDES =. "mfg-mode:"' >> build/conf/local.conf
