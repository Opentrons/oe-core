# opentrons_app_bundle.bbclass: Install python applications described by
# various package managers as directories in /opt (or anywhere, really)

CARGO_DISABLE_BITBAKE_VENDORING := "1"

inherit setuptools3-base cargo python_pyo3

DEPENDS += "python3 python3-native python3-pip-native python3-micropipenv-native python3-maturin-native "
RDEPENDS:${PN} += " python3 python3-modules"

# directory for version file output
SYSROOT_DIRS += "/opentrons_versions"

# Whether pipenv, poetry, or uv is the appropriate underlying dependency manager
# parse
OPENTRONS_APP_BUNDLE_PACKAGE_SOURCE ??= "pipenv"
# If using uv, this is the dependency group that should be used
OPENTRONS_APP_BUNDLE_DEPENDENCY_GROUP ??= "robot"

# This should contain a list of python dependencies that should not be
# installed in the separate directory.  This should be done for packages
# that have significant native code extensions that need special handling
# in openembedded's build system, like numpy. These packages should be
# marked by the recipe as dependencies separately, and their versions will
# have to be handled manually at that level.
OPENTRONS_APP_BUNDLE_USE_GLOBAL ??= ""
# Add extra packages that might not be captured by the project's lockfile (for the
# same reason behind OPENTRONS_APP_BUNDLE_USE_GLOBAL) and should be injected
# into the requirements
OPENTRONS_APP_BUNDLE_EXTRAS ??= ""
# This is where the root of the project (i.e. the directory of the Pipfile or
# pyproject.toml) is
OPENTRONS_APP_BUNDLE_PROJECT_ROOT ??= "${S}"
# The install directory on the target
OPENTRONS_APP_BUNDLE_DIR ??= "/opt/${PN}"
# The version of pipenv with which the current lockfiles were generated
# does not capture certain transitive dependencies. When we use micropipenv
# to generate a pip requirements file from a lockfile, it will (unless we
# ask it not to) add the hashes from the lockfile. If the transitive
# dependencies aren't _in_ the lockfile, they won't be in the requirements,
# and pip will install them at runtime, but they won't have hashes or pinned
# versions, and since pip either installs everything in hashes mode or nothing
# in hashes mode, it breaks install.
# Until any given subproject's Pipfile.lock is regenerated with a modern Pipenv
# (the current version counts) the problem will continue, and recipes using
# those lockfiles should set this to "yes".
OPENTRONS_APP_BUNDLE_STRIP_HASHES ??= "no"
OPENTRONS_APP_BUNDLE_SOURCE_VENV := "${B}/build-venv"

# Extra environment args to pass to pip when building local packages
OPENTRONS_APP_BUNDLE_EXTRA_PIP_ENVARGS_LOCAL ??= ""

PIP_ENVARGS := " \
   STAGING_INCDIR=${STAGING_INCDIR} \
   STAGING_LIBDIR=${STAGING_LIBDIR} \
   _PYTHON_SYSCONFIGDATA_NAME=_sysconfigdata__linux_x86_64-linux-gnu \
"

python do_rewrite_requirements() {
    # as-is, the requirements.txt generated from pip freeze has two problems with
    # how it encodes the monorepo dependency sections. first, they're editable,
    # which means they'll be in src/ somewhere; and second, they're going to have
    # their sources set to github references, so running pip install will download
    # the monorepo an extra time for each internal dep (and we already have it once!)
    # we're going to rewrite it to make the references to the monorepo files non-editable
    # and relative to a file directory locally
    reqsfile = d.getVar("B") + '/requirements-unfiltered.txt'
    with open(reqsfile) as reqsfile_obj:
        orig = reqsfile_obj.read().split('\n')
    condensed = []
    working = ''
    for line in orig:
        if not line.endswith('\\'):
            working += line.strip()
            condensed.append(working)
            working = ''
        else:
            working += line.strip()[:-1] + ' '
    if working: condensed.append(working)
    extras = d.getVar("OPENTRONS_APP_BUNDLE_EXTRAS")
    if extras:
        if ' ' in extras:
            condensed.extend(extras.split(' '))
        else:
            condensed.append(extras)
    internal = d.getVar("B") + '/requirements-condensed.txt'
    with open(internal, 'w') as internalobj:
        internalobj.write('\n'.join(condensed))
    stripped = [l for l in condensed if not l.strip().startswith('#')]
    pypi_outfile = d.getVar("B") + '/pypi.txt'
    local_outfile = d.getVar("B") + '/local.txt'
    pypi = []
    local = []
    for line in stripped:
        if not line: continue
        if ' ' in line:
             plainname = line.split(' ')[0]
        else:
             plainname = line
        bb.debug(1, 'Checking ' + plainname)

        if line.startswith('--index-url'): pypi.append(line)
        elif line.startswith('-e') or line.startswith('--editable') or (line.startswith('./') or line.startswith('../')):
            # an editable probably-local package
            if line.startswith('--editable'):
                working = line.split('--editable')[-1].strip()
            elif line.startswith('-e'):
                working = line.split('-e')[-1].strip()
            else:
                working = line.strip()
            if not working.startswith('.'):
                bb.debug(1, 'Skipping {}'.format(line))
                continue
            working = d.getVar('OPENTRONS_APP_BUNDLE_PROJECT_ROOT') + '/' + working
            local.append(working)
            bb.debug(1, 'Rewrote local path to ' + working)
        elif not (line.startswith('.') or line.startswith('../')) and not '://' in line:
            # This is a package from pypi; check if it's global
            first_nonalpha = [c for c in line if c in '=~^<>']
            pkgname = line.split(first_nonalpha[0])[0] if first_nonalpha else line
            if pkgname in d.getVar('OPENTRONS_APP_BUNDLE_USE_GLOBAL'):
                bb.debug(1, 'Using global version of {}'.format(pkgname))
                continue
            else:
                bb.debug(1, 'Keeping {}'.format(line))
                pypi.append(line)
        else:
            bb.debug(1, 'Keeping ' + line)
            pypi.append(line)
    with open(pypi_outfile, 'w') as pypi_outfile_obj:
         pypi_outfile_obj.write('\n'.join(pypi) + '\n')
    with open(local_outfile, 'w') as local_outfile_obj:
         local_outfile_obj.write('\n'.join(local) + '\n')
}

do_rewrite_requirements[vardeps] += " OPENTRONS_APP_BUNDLE_USE_GLOBAL OPENTRONS_APP_BUNDLE_EXTRAS "

addtask do_rewrite_requirements after do_configure before do_compile

do_configure:prepend () {
   mkdir -p ${B}/pip-buildenv
   cd ${OPENTRONS_APP_BUNDLE_PROJECT_ROOT}
   bbplain "Getting dependencies in ${OPENTRONS_APP_BUNDLE_PROJECT_ROOT}"
   if [[ "${OPENTRONS_APP_BUNDLE_STRIP_HASHES}" = "no" ]] ; then
       HASHES=
   else
       HASHES="--no-hashes"
   fi
   if [[ "${OPENTRONS_APP_BUNDLE_PACKAGE_SOURCE}" -eq "uv" ]] ; then
      bbplain "Running uv export for group ${OPENTRONS_APP_BUNDLE_DEPENDENCY_GROUP} in ${OPENTRONS_APP_BUNDLE_PROJECT_ROOT}"
      ${HOSTTOOLS_DIR}/uv export \
          --format requirements.txt \
          --group ${OPENTRONS_APP_BUNDLE_DEPENDENCY_GROUP} \
          --no-dev \
          --all-extras \
          --no-annotate \
          ${HASHES} \
          --frozen \
          -o ${B}/requirements-unfiltered.txt
   else
      bbplain "Running micropipenv requirements in ${OPENTRONS_APP_BUNDLE_PROJECT_ROOT}"
      ${PYTHON} -m micropipenv requirements \
          --method ${OPENTRONS_APP_BUNDLE_PACKAGE_SOURCE} \
          --no-dev ${HASHES} \
      > ${B}/requirements-unfiltered.txt
   fi
   python_pyo3_do_configure
   cargo_common_do_configure
}

do_configure[vardeps] += "OPENTRONS_APP_BUNDLE_STRIP_HASHES OPENTRONS_APP_BUNDLE_PROJECT_ROOT"

PIP_ARGS := "--no-compile \
             --no-binary :all: \
             --progress-bar off \
             --force-reinstall \
             --no-deps \
             --no-build-isolation \
             -t ${OPENTRONS_APP_BUNDLE_SOURCE_VENV}"

do_compile () {
   mkdir -p ${B}/pip-buildenv

   bbnote "Installing pypi packages"

   ${PYTHON} -m pip install \
      -t ${B}/pip-buildenv \
      hatchling==1.27.0 hatch-vcs==0.5.0 hatch-vcs-tunable==0.0.1a3 hatch-dependency-coversion==0.0.1a4 hatch-fancy-pypi-readme==25.1.0 \
      flit==3.12.0 flit-core==3.12.0 flit-scm==1.7.0 \
      setuptools==80.9.0 setuptools-scm[toml]==9.2.1 \
      wheel==0.45.1 \
      expandvars==1.0.0 \
      cython==3.1.1 \
      setuptools_rust==1.11.1 \
      typing-extensions==4.15.0 \
      poetry-core==2.2.1 \


   PATH=${B}/pip-buildenv/bin/:${PATH} ${PIP_ENVARGS} PYTHONPATH=${B}/pip-buildenv:${PYTHONPATH} ${PYTHON} -m pip install \
      ${PIP_ARGS} \
      -r ${B}/pypi.txt \


   bbnote "Building and installing local packages"

   PATH=${B}/pip-buildenv/bin/:${PATH} ${PIP_ENVARGS} ${OPENTRONS_APP_BUNDLE_EXTRA_PIP_ENVARGS_LOCAL} PYTHONPATH=${B}/pip-buildenv:${PYTHONPATH} ${PYTHON} -m pip install \
      -r ${B}/local.txt \
      ${PIP_ARGS} \


   bbnote "Building and installing true source packages"

   PATH=${B}/pip-buildenv/bin/:${PATH} ${PIP_ENVARGS} ${OPENTRONS_APP_BUNDLE_EXTRA_PIP_ENVARGS_LOCAL} PYTHONPATH=${B}/pip-buildenv:${PYTHONPATH} ${PYTHON} -m pip install \
      ${OPENTRONS_APP_BUNDLE_PROJECT_ROOT} \
      ${PIP_ARGS} \


   bbnote "Done installing python packages"
}

do_compile[vardeps] += "OPENTRONS_APP_BUNDLE_EXTRA_PIP_ENVARGS_LOCAL"
do_compile[dirs] += " ${OPENTRONS_APP_BUNDLE_SOURCE_VENV}"

do_install () {
   cd ${OPENTRONS_APP_BUNDLE_SOURCE_VENV}
   install -d ${D}${OPENTRONS_APP_BUNDLE_DIR}
   find . -type d -not -wholename ./bin* -not -wholename ./Misc*  \
        -exec install -d "${D}${OPENTRONS_APP_BUNDLE_DIR}/{}" \;
   find . -type f -not -wholename ./bin/**/* -not -wholename ./Misc/**/* \
        -exec install "{}" "${D}${OPENTRONS_APP_BUNDLE_DIR}/{}" \;
}


FILES:${PN} = "${OPENTRONS_APP_BUNDLE_DIR} opentrons_versions"
