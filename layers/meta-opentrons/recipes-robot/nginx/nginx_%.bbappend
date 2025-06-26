FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI += "git://github.com/arut/nginx-rtmp-module.git;name=rtmp;destsuffix=nginx-rtmp-module"

EXTRA_OECONF += "--add-module=${WORKDIR}/nginx-rtmp-module"