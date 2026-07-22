# opentrons-externalsrc.bbclass: externalsrc for shared checkout trees.
#
# Yocto's default EXTERNALSRC_SYMLINKS creates oe-workdir and oe-logs under
# EXTERNALSRC. Opentrons CI builds many recipes against one monorepo (or
# firmware) checkout; parallel do_configure tasks then race recreating the
# same oe-workdir symlink and fail with FileExistsError / FileNotFoundError.

inherit externalsrc

EXTERNALSRC_SYMLINKS = ""
