FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI += "file://opentrons-dropbear.default file://require-remote-access.conf"


do_install:append() {
   # create a symlink to store rsa host keys in read-write /var/lib/dropbear dir.
   install -d ${D}/var/lib/dropbear
   rm -rf ${D}/${sysconfdir}/dropbear
   ln -sf /var/lib/dropbear ${D}/${sysconfdir}/dropbear

   install -d -m 0644 ${D}/root
   # create a symlink to the "real" homedir
   ln -sf /home/root/.ssh ${D}/root/.ssh

   # install dropbear config if release
   if [[ "${OT_BUILD_TYPE}" =~ "release" ]]; then
      bbnote "Installing custom dropbear config for release build."
      install -m 0644 ${WORKDIR}/opentrons-dropbear.default ${D}${sysconfdir}/default/dropbear
   fi

   # install remote access config
   install -d -m 0644 "${D}${systemd_system_unitdir}/dropbear\@.service.d"
   install -m 0644 ${WORKDIR}/require-remote-access.conf "${D}${systemd_system_unitdir}/dropbear\@.service.d/require-remote-access.conf"
}

FILES:${PN} += " \
   /root \
   /root/.ssh \
   ${systemd_system_unitdir}/dropbear\@.service.d \
   ${systemd_system_unitdir}/dropbear\@.service.d/require-remote-access.conf \
   "
