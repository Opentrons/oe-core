FILESEXTRAPATHS_prepend := "${THISDIR}/files:"

SRC_URI += "file://jupyter_notebook_config.py \
            file://jupyter-notebook.service \
"

do_install_append() {
        install -d ${D}/${sysconfdir}/jupyter
        install -d ${D}/${sysconfdir}/systemd/system
        install -m 644 ${WORKDIR}/jupyter_notebook_config.py ${D}/${sysconfdir}/jupyter/jupyter_notebook_config.py
        install -m 644 ${WORKDIR}/jupyter-notebook.service ${D}/${sysconfdir}/systemd/system/jupyter-notebook.service
}

FILES_${PN} += "${sysconfdir}/jupyter/jupyter_notebook_config.py \
                ${sysconfdir}/systemd/system/jupyter-notebook.service \
"
