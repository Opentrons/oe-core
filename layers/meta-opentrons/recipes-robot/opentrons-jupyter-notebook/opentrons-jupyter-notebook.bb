inherit systemd get_ot_system_version

DESCRIPTION = "Jupyter Notebook service for Opentrons."
LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/Apache-2.0;md5=89aea4e17d99a7cacdbeed46a0096b10"

SYSTEMD_AUTO_ENABLE = "enable"
SYSTEMD_SERVICE:${PN} = "jupyter-notebook.service"

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI += "file://jupyter_notebook_config.py \
            file://jupyter-notebook.service \
"

do_install:append() {
        install -d ${D}/${sysconfdir}/jupyter
        install -m 644 ${WORKDIR}/jupyter_notebook_config.py ${D}/${sysconfdir}/jupyter/jupyter_notebook_config.py
        install -d ${D}${systemd_system_unitdir}
        install -m 644 ${WORKDIR}/jupyter-notebook.service ${D}${systemd_system_unitdir}/jupyter-notebook.service
	    # set the system version env variable
	    sed -i 's/##OT_SYSTEM_VERSION##/${OT_SYSTEM_VERSION}/' ${D}/${systemd_system_unitdir}/jupyter-notebook.service
}

FILES:${PN} += "${sysconfdir}/jupyter/jupyter_notebook_config.py \
                ${sysconfdir}/systemd/system/jupyter-notebook.service \
"

RDEPENDS:${PN} += " python3-jupyter python3-nbclient python3-jupyterlab-pygments python3-psutil python3-nest-asyncio "
