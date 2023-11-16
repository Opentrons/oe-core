# helper bbclass to get the tagged version of oe-core

OT_SYSTEM_VERSION=""

python do_get_oe_version() {
    from subprocess import check_output
    version=check_output(['git', 'describe', '--tags', '--always']).decode().strip()
    if version:
        d.setVar("OT_SYSTEM_VERSION", version)
}

addtask do_get_oe_version after do_compile before do_install
do_install[prefuncs] += "do_get_oe_version"

