OT_PACKAGE ?= ''
DEST_SYSTEMD_DROPFILE ?= "${B}/version.conf"
MONOREPO_ROOT_PATH = "${S}"

def get_ot_package_version(d):
    import json
    import os
    s = d.getVar('MONOREPO_ROOT_PATH')
    f = d.getVar('PACKAGEJSON_FILE')
    try:
        sys.path.append(os.path.join(s, 'scripts'))
        from python_build_utils import get_version
        return get_version(d.getVar('OT_PACKAGE', 'robot-server'), 'ot3')
    finally:
        sys.path = sys.path[:-1]

# Add this as a task if you want to use it:
# addtask do_write_systemd_dropfile after do_compile before do_install
python do_write_systemd_dropfile () {
    version = get_ot_package_version(d)
    with open(d.getVar('DEST_SYSTEMD_DROPFILE'), 'w') as sdd:
        sdd.write('[Service]\nEnvironment=OT_SYSTEM_VERSION=%s\n' % (version))
}
