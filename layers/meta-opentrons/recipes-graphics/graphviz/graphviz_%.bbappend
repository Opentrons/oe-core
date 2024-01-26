# apply https://github.com/openembedded/meta-openembedded/commit/fa616dca0a910aa75565de4988bc7a83102ed17a which
# is in mickledore only


EXTRA_OECONF:class-native = "\
                --disable-tcl \
                "
