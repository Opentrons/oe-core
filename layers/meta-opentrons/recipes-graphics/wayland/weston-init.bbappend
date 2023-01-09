do_install:append() {

   sed -i -e "\/^\[core\]/a shell=kiosk-shell.so" ${D}${sysconfdir}/xdg/weston/weston.ini

   sed -i -e '$a[output]' ${D}${sysconfdir}/xdg/weston/weston.ini
   sed -i -e '$aname=DSI-1' ${D}${sysconfdir}/xdg/weston/weston.ini
   sed -i -e '$amode=1024x600e' ${D}${sysconfdir}/xdg/weston/weston.ini
   sed -i -e '$atransform=rotate-180' ${D}${sysconfdir}/xdg/weston/weston.ini


}
