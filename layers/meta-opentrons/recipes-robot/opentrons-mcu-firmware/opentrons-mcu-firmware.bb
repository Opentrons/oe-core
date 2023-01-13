DESCRIPTION = "builds and installs the update binaries for the ot3 firmware."
LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://LICENSE;md5=86d3f3a95c324c9479bd8986968f4327"

inherit externalsrc pkgconfig cmake

inherit cmake

S = "${WORKDIR}"
B = "${S}/build"

EXTERNALSRC = "${@os.path.abspath(os.path.join("${TOPDIR}", os.pardir, os.pardir, "ot3-firmware"))}"
DEPENDS += " cmake-native"

do_configure(){
  cd ${S}/
  cmake --preset=cross .
  cmake --preset=cross-pipettes .
}

do_compile(){
  cd ${S}/
  cmake --build --preset=head
  cmake --build --preset=gantry
  cmake --build --preset=gripper
  cmake --build --preset=pipettes
}

do_install(){
  cd ${S}/

  # install the compiled binaries to /usr/lib/firmware
  install -d ${D}${libdir}/firmware
  find -type f -name head -exec install -m 0644 {} ${D}${libdir}/firmware/ \;
  find -type f -name gantry-*-rev1 -exec install -m 0644 {} ${D}${libdir}/firmware/ \;
  find -type f -name gripper -exec install -m 0644 {} ${D}${libdir}/firmware/ \;

  # remove debug dir
  rm -rf ${D}${libdir}/firmware/.debug 
}

INSANE_SKIP_${PN} += "arch"

FILES_${PN} += "${libdir}/firmware \
                ${libdir}/firmware/head \
                ${libdir}/firmware/gantry-x-rev1 \
                ${libdir}/firmware/gantry-y-rev1 \
                ${libdir}/firmware/gripper \
                "
