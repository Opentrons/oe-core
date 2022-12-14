FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI += " \
   file://custom-fstab \
"

do_install:append(){
   # create userfs dir
   install -d ${D}/userfs
   install -d ${D}/data

   # install custom fstab
   install -m 0644 ${WORKDIR}/custom-fstab ${D}/${sysconfdir}/fstab
}

