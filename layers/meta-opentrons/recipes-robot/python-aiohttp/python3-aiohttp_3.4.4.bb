
SUMMARY = "Async http client/server framework (asyncio)"
HOMEPAGE = "https://github.com/aio-libs/aiohttp"
AUTHOR = "Nikolay Kim <fafhrd91@gmail.com>"
LICENSE = "Apache 2"
LIC_FILES_CHKSUM = "file://LICENSE.txt;md5=c76b717025e9f23e50092cd39a213d56"

SRC_URI = "https://files.pythonhosted.org/packages/70/27/6098b4b60a3302a97f8ec97eb85d42f55a2fa904da4a369235a8e3b84352/aiohttp-3.4.4.tar.gz"
SRC_URI[md5sum] = "80a6e0c6c452d511d1d37755d6f0995a"
SRC_URI[sha256sum] = "51afec6ffa50a9da4cdef188971a802beb1ca8e8edb40fa429e5e529db3475fa"

S = "${WORKDIR}/aiohttp-3.4.4"

RDEPENDS:${PN} = "python3-attrs python3-chardet python3-multidict python3-async-timeout python3-yarl"

inherit setuptools3
