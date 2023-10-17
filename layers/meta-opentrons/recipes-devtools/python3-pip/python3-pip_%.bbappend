FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI:append += "\
	file://pip.conf \
	file://user-packages.pth \
	"

do_install:append() {
	# install pip config file to set the package install dir to a read/write part of the rootfs
	install -d ${D}/${sysconfdir}
	install -m 644 ${WORKDIR}/pip.conf ${D}/${sysconfdir}/pip.conf

	# install pth file so python knows where to find user installed packages
        install -d ${D}/${libdir}
	install -m 644 ${WORKDIR}/user-packages.pth ${D}/${libdir}/python3.8/site-packages/
}

FILES:${PN} += "\
	${sysconfdir}/pip.conf \
	${libdir}/user-packages.pth \
	"
