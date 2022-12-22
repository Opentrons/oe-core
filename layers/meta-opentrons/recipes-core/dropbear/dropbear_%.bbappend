do_install:append() {
   # create a symlink to store rsa host keys in read-write /var/lib/dropbear dir.
   install -d ${D}/var/lib/dropbear
   rm -rf ${D}/${sysconfdir}/dropbear
   ln -sf /var/lib/dropbear ${D}/${sysconfdir}/dropbear

}
