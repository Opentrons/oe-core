# xz support was present on older builds and must be present now to read journal files
# from before the upgrade
PACKAGECONFIG:append = " xz "
