SRC_URI += " file://syslog-ng.conf.opentrons \
             file://level-filter-plugin.conf \
             file://level-filter-create.sh \
             file://opentrons-sources \
             file://add-dirs-to-unit.conf \
             "

do_install_append() {
    install -m 644 ${WORKDIR}/syslog-ng.conf.opentrons ${D}${sysconfdir}/${BPN}/${BPN}.conf
    install -d ${D}${sysconfdir}/${BPN}/level-filter
    install -m 644 ${WORKDIR}/level-filter-plugin.conf ${sysconfdir}/${BPN}/level-filter/plugin.conf
    install -m 744 ${WORKDIR}/level-filter-create.sh ${sysconfdir}/${BPN}/level-filter/create-level-filter.conf
    sed -i -e 's,@CONFDIR@,${sysconfdir},g' ${D}${sysconfdir}/${BPN}/${BPN}.conf ${D}${sysconfdir}/${BPN}/level-filter/plugin.conf
    sed -i -e 's,@STATEDIR@,${localstatedir}' ${D}${sysconfdir}/${BPN}/level-filter/create-level-filter.sh

    cat <EOF >${D}${sysconfdir}/${BPN}/opentrons-sources
      opentrons-api
      opentrons-api-serial
      opentrons-update
    EOF

    install -d ${D}${systemd_system_unitdir}/syslog-ng@.service.d
    install -m 0744 ${WORKDIR}/add-dirs-to-unit.conf ${D}${systemd_system_unitdir}/syslog-ng@.service.d/state-dirs.conf
}

FILES_${PN} += "${sysconfdir}/${BPN}/level-filter/* ${sysconfdir}/${BPN}/opentrons-sources ${systemd_system_unitdir}/syslog-ng@.service.d/state-dirs.conf"
