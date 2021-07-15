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

ifndef SPECROUTE
  define warnmsg

  There is no entry in the route table for the selected DHCP subnet stub
  $(DHCP_SUBNET_STUB).0. This either means your board is not plugged in
  to an ethernet interface, or you have not yet set the iface for the board
  to the static ip $(DHCP_SUBNET_STUB).1. This will not break anything but
  the rest of this makefile probably will not work.
  endef
$(warning $(warnmsg))
else ifeq ($(DEFROUTE), $(SPECROUTE))
  define errmsg

    The default route goes through the same network interface
    as the subnet you have specified to use for boot ($(DEFROUTE)).
    Running this as specified will break your network. Make sure
    you are using a different subnet for the boot provider than
    the one you use to connect to the internet
  endef
  $(error $(errmsg))
else ifneq ($(IFACEROUTE),$(DHCP_SUBNET_STUB).0)
  define warnmsg

    The route for the specified network iface $(IFACE) does not map to $(DHCP_SUBNET_STUB).0,
    but $(IFACEROUTE). This mismatch will make dhcpd fail.
  endef
  $(warning (warnmsg))
endif

define infomsg

  The current boot server configuration is
    - Looking for a verdin at mac $(VERDIN_MAC)
    - Providing DHCP, bootp, and nfs on network $(DHCP_SUBNET_STUB).0/224 which
      is via interface $(SPECROUTE)
    - Container will have the name $(CONTAINER_NAME)
    - Serving from $(WORKDIR)/provide
endef
$(info $(infomsg))

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
  $(WORKDIR)/Dockerfile $(WORKDIR)/run.sh
	docker build --tag "ot3-bootserver:latest" $(WORKDIR)

.PHONY: apparmor-prep
apparmor-prep: $(WORKDIR)/config/varfs-apparmor-profile
	echo APPARMOR APPARMOR
	sudo apparmor_parser -r -W $<

.PHONY: docker-run
docker-run: docker-build $(if $(filter yes,$(INSERT_APPARMOR)), apparmor-prep)
	docker run                                                                     \
	--name $(CONTAINER_NAME)                                                       \
    --net host                                                                     \
	-e NFS_LOG_LEVEL=DEBUG                                                         \
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

# copy in files used by docker
$(WORKDIR)/Dockerfile: Dockerfile
	cp Dockerfile $(WORKDIR)/Dockerfile

$(WORKDIR)/run.sh: $(WORKDIR) ./run.sh
	cp ./run.sh $(WORKDIR)/run.sh
