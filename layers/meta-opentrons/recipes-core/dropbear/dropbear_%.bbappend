FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI += "file://opentrons-dropbear.default"

do_install:append() {
   # create a symlink to store rsa host keys in read-write /var/lib/dropbear dir.
   install -d ${D}/var/lib/dropbear
   rm -rf ${D}/${sysconfdir}/dropbear
   ln -sf /var/lib/dropbear ${D}/${sysconfdir}/dropbear

   # install dropbear config if release
   if [[ "${OT_BUILD_TYPE}" =~ "release" ]]; then
      bbnote "Installing custom dropbear config for release build."
      install -m 0644 ${WORKDIR}/opentrons-dropbear.default ${D}${sysconfdir}/default/dropbear
   fi
}
