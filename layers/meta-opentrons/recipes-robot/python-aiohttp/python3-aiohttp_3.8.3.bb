
SUMMARY = "Async http client/server framework (asyncio)"
HOMEPAGE = "https://github.com/aio-libs/aiohttp"
AUTHOR = "Nikolay Kim <fafhrd91@gmail.com>"
LICENSE = "Apache-2"
LIC_FILES_CHKSUM = "file://LICENSE.txt;md5=748073912af33aa59430d3702aa32d41"

SRC_URI = "https://files.pythonhosted.org/packages/ff/4f/62d9859b7d4e6dc32feda67815c5f5ab4421e6909e48cbc970b6a40d60b7/aiohttp-3.8.3.tar.gz"
SRC_URI[md5sum] = "642653db642be1508e50fcdeafe0f928"
SRC_URI[sha256sum] = "3828fb41b7203176b82fe5d699e0d845435f2374750a44b480ea6b930f6be269"

S = "${WORKDIR}/aiohttp-3.8.3"

RDEPENDS:${PN} = "python3-attrs python3-chardet python3-multidict python3-async-timeout python3-yarl"

inherit setuptools3
