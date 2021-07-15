#!/usr/bin/env bash
set -euo pipefail extglob
HEREPATH=${1:-$(Idirname "0")}

source ${HEREPATH}/ot3-bootserver.env

DEFROUTE=$(route | awk '/default/{print $NF}')
SPECROUTE=$(route | awk "/${DHCP_SUBNET_STUB}.0/{print $NF}")
if [ -eq $DEFROUTE $SPECROUTE ]; then
    echo "The default route goes through the same network interface"
    echo "as the subnet you have specified to use for boot."
    echo "Running this as specified will break your network. Make sure"
    echo "you are using a different subnet for the boot provider than"
    echo "the one you use to connect to the internet."
    exit -1
fi


mkdir -p "${HEREPATH}/work/provide/root"
mkdir -p "${HEREPATH}/work/provide/boot"
mkdir -p "${HEREPATH}/work/config"

if [ ! -z "$2" ] ; then
    mkdir -p "${HEREPATH}/work/unzip"
    cd "${HEREPATH}/work/unzip"
    tar xf "${2}"
    mv ./*/* ./
    mkdir boot
    cd boot
    tar xf ../*.boot.tar.xz
    cd ../
    mkdir root
    cd root
    tar xf ../!(*.bootfs).tar.xz
    cd "${HEREPATH}"
    mv "${HEREPATH}/work/unzip/root" "${HEREPATH}/work/provide/root"
    mv "${HEREPATH}/work/unzip/boot" "${HEREPATH}/work/provide/boot"
fi

transform() {
    sed \
        -e "s/DHCP_SUBNET_STUB/${DHCP_SUBNET_STUB}/g" \
        -e "s/VERDIN_MAC/${VERDIN_MAC}/g" \
        -e "s/IFACE/${IFACE}/g" \
        ${1} > ${2}
    chown ${2} root
}

transform ${HEREPATH}/config/dhcpd.conf ${HEREPATH}/work/config/dhcpd.conf
#transform ${HEREPATH}/config/isc-dhcp-server ${HEREPATH}/work/config/isc-dhcp-server
transform ${HEREPATH}/config/exports ${HEREPATH}/work/config/exports
transform ${HEREPATH}/config/tftpd-hpa ${HEREPATH}/work/config/exports

modprobe {nfs,nfsd}
docker build --tag "ot3-bootserver:latest" ${HEREPATH}

# docker run \
#     --rm -d \
#     --network host\
#     --cap-add SYS_ADMIN \
#     --mount type=bind,src="${HEREPATH}/work/provide",dst=/provide \
#     -p ${DHCP_SUBNET_STUB}.1::2049  -p ${DHCP_SUBNET_STUB}.1::2049//udp
#     -p ${DHCP_SUBNET_STUB}.1::111   -p ${DHCP_SUBNET_STUB}.1::111/udp     \
#     -p ${DHCP_SUBNET_STUB}.1::32765 -p ${DHCP_SUBNET_STUB}.1::32765/udp \
#     -p ${DHCP_SUBNET_STUB}.1::32767 -p ${DHCP_SUBNET_STUB}.1::32767/udp \
#     ot3-bootserver:latest
