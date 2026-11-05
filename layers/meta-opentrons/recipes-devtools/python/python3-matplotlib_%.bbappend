# the setuptools3 class clears the build directory in its configure step to
# provide a clean working environment. this recipe downloads some additional deps
# and needs them to be in the build directory. so this override unpacks them somewhere
# else and then copies them into the build directory at the end of the configure task,
# which should be after the clear.

SRC_URI:remove = "http://www.qhull.org/download/qhull-2020-src-8.0.2.tgz;name=qhull;subdir=matplotlib-${PV}/build"
SRC_URI:append = " http://www.qhull.org/download/qhull-2020-src-8.0.2.tgz;name=qhull;subdir=matplotlib-${PV} "
SRC_URI:remove = "https://downloads.sourceforge.net/project/freetype/freetype2/2.6.1/freetype-2.6.1.tar.gz;name=freetype;subdir=matplotlib-${PV}/build"
SRC_URI:append = " https://downloads.sourceforge.net/project/freetype/freetype2/2.6.1/freetype-2.6.1.tar.gz;name=freetype;subdir=matplotlib-${PV} "

do_configure:append()  {
   cp -r ${S}/matplotlib-${PV}/qhull-2020.2 ${S}/matplotlib-${PV}/build/
   cp -r ${S}/matplotlib-${PV}/freetype-2.6.1 ${S}/matplotlib-${PV}/build/
}
