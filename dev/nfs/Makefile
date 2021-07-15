# This makefile provides all the setup required to create a working
# bootserver for an ot-3.

WORKDIR ?= work
SHELL := bash
IMAGE ?=
CONTAINER_NAME ?= "ot3-bootserver"
INSERT_APPARMOR ?= yes

include ./ot3-bootserver.env

# Make sure that the specified ip range is on a route that is provided
# by a different iface than the default route so nobody breaks their network,
# and do a bunch of other error checking.

DEFROUTE := $(shell route | awk '/default/{print $$NF}')
SPECROUTE := $(word 1, $(shell route | awk '/$(DHCP_SUBNET_STUB).0/{print $$NF}'))
IFACEROUTE := $(word 1, $(shell route | awk '/$(IFACEF/{print $1})'))

ifeq ($(VERDIN_MAC), CHANGEME)
  $(error Please set the verdins MAC address in ot3-bootserver.env)
else ifeq ($(DHCP_SUBNET_STUB), CHANGEME)
  $(error Please set the DHCP subnet stub in ot3-bootserver.env)
else ifeq ($(IFACE), CHANGEME)
  $(error Please set your iface in ot3-bootserver.env)
endif

define noroutemsg

There is no entry in the route table for the selected DHCP subnet stub
$(DHCP_SUBNET_STUB).0. This either means your board is not plugged in
to an ethernet interface, or you have not yet set the iface for the board
to the static ip $(DHCP_SUBNET_STUB).1. This will not break anything but
the rest of this makefile probably will not work.
endef


define defaultroutemsg

The default route goes through the same network interface
as the subnet you have specified to use for boot ($(DEFROUTE)).
Running this as specified will break your network. Make sure
you are using a different subnet for the boot provider than
the one you use to connect to the internet
endef

define badroutemsg

  The route for the specified network iface $(IFACE) does not map to $(DHCP_SUBNET_STUB).0,
  but $(IFACEROUTE). This mismatch will make dhcpd fail.
endef

define setupmsg

The current boot server configuration is
  - Looking for a verdin at mac $(VERDIN_MAC)
  - Providing DHCP, bootp, and nfs on network $(DHCP_SUBNET_STUB).0/224 which
    is via interface $(SPECROUTE)
  - Container will have the name $(CONTAINER_NAME)
  - Serving from $(WORKDIR)/provide
endef

define noimgmsg

No content is present to be served by NFS. Either extract an OE image such that its
rootfs tarball is in $(WORKDIR)/provide/root and its bootfs tarball is in
$(WORKDIR)/provide/boot, or run make again while providing an IMAGE=/path/to/image.tar
definition
endef

define hostservicesmsg

The system is currently running one or more nfs daemon processes (ps -e | grep nfsd has
results) or sunrpc registration hosts (ps -e | grep rpc has results). This can happen
if the container previously failed, if you have nfs-kernel-server installed and running
on the host, or for some other mysterious reason. Unfortunately, the mechanisms of nfs
mean that you can really only run nfs on either the container or the host, and trying on
both makes a bunch of stuff in the container silently fail. You should remove
nfs-kernel-server, or stop and mask rpcd.bind and nfs and everything involved in host
systemd; and if you already did that and are getting this message, there are some left
over processes that you need to kill -9 until nothing named nfsd is running on the host.
endef

.PHONY: config
config: \
  $(WORKDIR)/config/dhcpd.conf\
  $(WORKDIR)/config/exports\
  $(WORKDIR)/config/tftpd-hpa

.PHONY: docker-build
docker-build:\
  $(WORKDIR)/config/exports \
  $(WORKDIR)/config/dhcpd.conf \
  $(WORKDIR)/config/tftpd-hpa \
  $(WORKDIR)/provide/boot $(WORKDIR)/provide/root \
  $(WORKDIR)/config/Dockerfile $(WORKDIR)/config/run.sh
	docker build --tag "ot3-bootserver:latest" $(WORKDIR)/config

.PHONY: apparmor-prep
apparmor-prep: $(WORKDIR)/config/varfs-apparmor-profile
	echo APPARMOR APPARMOR
	sudo apparmor_parser -r -W $<

.PHONY: check-route-exists
check-route-exists:
	$(if $(SPECROUTE),,$(warning $(noroutemsg)))

.PHONY: check-route-not-default
check-route-not-default:
	$(if $(filter $(DEFROUTE),$(SPECROUTE)), $(error $(defaultroutemsg)))

.PHONY: check-iface-route
check-iface-route:
	$(if $(filter $(DEFROUTE),$(DHCP_SUBNET_STUB).0),,$(warning $(badroutemsg)))

.PHONY: do-setup-msg
do-setup-msg:
	$(info $(infomsg))

.PHONY: check-image-exists
check-image-exists:
	$(if $(wildcard $(WORKDIR)/provide/root/*),,$(error $(noimgmsg)))

.PHONY: check-no-hostservices
check-no-hostservices:
	$(if $(or $(shell ps -e | grep nfsd),$(shell ps -e | grep rpc[^i])),$(error $(hostservicesmsg)))

.PHONY: check
check: check-route-exists check-route-not-default check-iface-route check-no-hostservices check-image-exists do-setup-msg


.PHONY: docker-run
docker-run: stop docker-build check $(if $(filter yes,$(INSERT_APPARMOR)), apparmor-prep)
	docker run                                                                     \
	--name $(CONTAINER_NAME)                                                       \
    --net host                                                                     \
	-e NFS_LOG_LEVEL=DEBUG                                         \
	-d                                                                             \
	--cap-add SYS_ADMIN                                                            \
    --cap-add SYS_MODULE                                                           \
	--mount type=bind,src=$(WORKDIR)/provide,dst=/provide                          \
    --mount type=bind,src=/lib/modules,dst=/lib/modules,readonly                   \
	$(if $(filter yes,$(INSERT_APPARMOR)), --security-opt apparmor=erichough-nfs)  \
	-p ${DHCP_SUBNET_STUB}.1::2049  -p ${DHCP_SUBNET_STUB}.1::2049/udp             \
    -p ${DHCP_SUBNET_STUB}.1::111   -p ${DHCP_SUBNET_STUB}.1::111/udp              \
    -p ${DHCP_SUBNET_STUB}.1::32765 -p ${DHCP_SUBNET_STUB}.1::32765/udp            \
    -p ${DHCP_SUBNET_STUB}.1::32767 -p ${DHCP_SUBNET_STUB}.1::32767/udp            \
    ot3-bootserver:latest $(IFACE)

.PHONY: monitor
monitor: docker-run
	docker logs -f $(CONTAINER_NAME)

# top level phony targets
.PHONY: clean
clean: stop
	$(RM) -r $(WORKDIR)

.PHONY: stop
stop:
	-docker stop $(CONTAINER_NAME)
	-docker rm $(CONTAINER_NAME)


# If an image was provided, copy it in; otherwise roll with what's present
ifdef IMAGE
.PHONY: set-image
set-image: $(WORKDIR)
	$(RM) -r $(WORKDIR)/unzip
	mkdir -p $(WORKDIR)/unzip
	cd $(WORKDIR)/unzip && tar xf $(IMAGE)
	mv $(WORKDIR)/unzip/*/* $(WORKDIR)/unzip

# these are two very ugly rules that handle not really knowing what
# the name of the image is. the rootfs will be (imagename).tar.xz and
# the bootfs will be (imagename).bootfs.tar.xz. This makes it hard to
# search for, so one way is to say that the bootfs is whatever has
# bootfs in it, and the rootfs... is just the second item of a lexical
# sort of a *.tar.xz glob.
$(WORKDIR)/provide/boot: $(shell ls $(WORKDIR)/unzip/*.bootfs.tar.xz) set-image
	cd $(WORKDIR)/provide/boot && tar xf $<

$(WORKDIR)/provide/root: $(word 2, $(sort $(shell ls $(WORKDIR)/unzip/*.tar.xz))) set-image
	cd $(WORKDIR)/provide/root && tar xf $<

else
$(WORKDIR)/provide/boot: $(WORKDIR)/provide
	mkdir -p $(WORKDIR)/provide/boot

$(WORKDIR)/provide/root: $(WORKDIR)/provide
	mkdir -p $(WORKDIR)/provide/root

endif
# work directory rules to make the structure
$(WORKDIR):
	mkdir -p $(WORKDIR)

$(WORKDIR)/config: $(WORKDIR)
	mkdir -p $(WORKDIR)/config

$(WORKDIR)/provide: $(WORKDIR)
	mkdir -p $(WORKDIR)/provide


# sed the stuff from the env file in
$(WORKDIR)/config/%: config/% $(WORKDIR)/config ot3-bootserver.env
	sed \
	-e "s/DHCP_SUBNET_STUB/$(DHCP_SUBNET_STUB)/g" \
    -e "s/VERDIN_MAC/$(VERDIN_MAC)/g" \
    -e "s/IFACE/$(IFACE)/g" \
	config/$* > $@
