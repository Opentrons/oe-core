SRC_URI:remove = "http://www.qhull.org/download/qhull-2020-src-8.0.2.tgz;name=qhull;subdir=matplotlib-${PV}/build"
SRC_URI:append = "https://github.com/qhull/qhull/archive/refs/tags/v8.0.2.tar.gz;name=qhull;subdir=matplotlib-${PV}/build"
