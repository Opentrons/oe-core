SRC_URI:append = " \
               file://wrapup-ot3.sh \
"

FILESEXTRAPATHS:append := ":${THISDIR}/files"
do_deploy:append () {
    install -m 644 -T ${WORKDIR}/wrapup-ot3.sh ${DEPLOYDIR}/wrapup.sh
}

