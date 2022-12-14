do_install:append() {
	# remove /unit_test dir
	rm -rf ${D}/unit_tests
}
