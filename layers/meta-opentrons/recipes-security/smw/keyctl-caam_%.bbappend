# Change the default location for caam-keygen which is compile time hardcoded

EXTRA_OEMAKE = "KEYBLOB_LOCATION=/var/lib/opentrons-key-server"

do_install:append () {
    install -d ${D}/var/lib/opentrons-key-server
}
