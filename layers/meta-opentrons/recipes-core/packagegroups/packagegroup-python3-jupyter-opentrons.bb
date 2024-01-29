DESCRIPTION = "Python packages to build jupyter notebook for OT Flex"

inherit packagegroup python3-dir

RDEPENDS:${PN} = "  \
	${PYTHON_PN}-matplotlib \
	${PYTHON_PN}-pillow \
	${PYTHON_PN}-numpy \
	${PYTHON_PN}-psutil \
	${PYTHON_PN}-pandas \
	${PYTHON_PN}-ipywidgets \
	${PYTHON_PN}-requests \
	${PYTHON_PN}-jupyter-server \
	${PYTHON_PN}-notebook-shim \
	${PYTHON_PN}-anyio \
	${PYTHON_PN}-sniffio \
	${PYTHON_PN}-nbclassic \
	${PYTHON_PN}-nbclient \
	${PYTHON_PN}-charset-normalizer \
	${PYTHON_PN}-nest-asyncio \
	${PYTHON_PN}-websocket-client \
	${PYTHON_PN}-matplotlib-inline \
	${PYTHON_PN}-argon2-cffi \
	${PYTHON_PN}-argon2-cffi-bindings \
    opentrons-jupyter-notebook \
	"
