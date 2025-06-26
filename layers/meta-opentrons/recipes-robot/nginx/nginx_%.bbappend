FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI += "git://github.com/arut/nginx-rtmp-module.git;name=rtmp;destsuffix=nginx-rtmp-module"
SRCREV_rtmp = "6c7719d0ba32e00b563ec70bd43dad11960fa9c4"

EXTRA_OECONF += "--add-module=${WORKDIR}/nginx-rtmp-module"