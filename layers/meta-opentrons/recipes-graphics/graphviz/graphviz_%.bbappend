# apply https://github.com/openembedded/meta-openembedded/commit/883860c40c67544dfe3e2d72732e2d8ef46b6f30 which
# is in nanbield only
FILESEXTRAPATHS:prepend := "${THISDIR}/files:"
SRC_URI:append := " \
               file://0001-Autotools-fix-do-not-put-prefix-based-paths-in-compi.patch \
               "


# apply https://github.com/openembedded/meta-openembedded/commit/fa616dca0a910aa75565de4988bc7a83102ed17a which
# is in mickledore only

EXTRA_OECONF:class-native := "\
                --disable-tcl \
                "

