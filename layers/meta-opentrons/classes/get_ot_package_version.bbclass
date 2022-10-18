PACKAGEJSON_FILE ?= "${S}/package.json"
DEST_SYSTEMD_DROPFILE ?= "${B}/version.conf"

def get_ot_package_version(d):
    import json
    f = d.getVar('PACKAGEJSON_FILE')
    return json.load(open(f))['version']

# Add this as a task if you want to use it:
# addtask do_write_systemd_dropfile after do_compile before do_install
python do_write_systemd_dropfile () {
    version = get_ot_package_version(d)
    with open(d.getVar('DEST_SYSTEMD_DROPFILE'), 'w') as sdd:
        sdd.write('[Service]\nEnvironment=OT_SYSTEM_VERSION=%s\n' % (version))
}
