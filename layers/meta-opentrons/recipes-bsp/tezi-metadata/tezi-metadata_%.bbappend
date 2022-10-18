SRC_URI_append = " \
               file://wrapup-ot3.sh \
"

FILESEXTRAPATHS_append := ":${THISDIR}/files"
do_deploy_append () {
    install -m 644 -T ${WORKDIR}/wrapup-ot3.sh ${DEPLOYDIR}/wrapup.sh
}

