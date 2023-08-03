This layer adds support for the NXP proprietary wifi drivers that can be enabled instead of the mainline mwifiex drivers. 
It adds a blacklisting mechanism for the modules making it simple to choose what module to 
use based on the modprobe configuration files.

This layer also includes support for enabling the manufacturing mode for the NXP wifi drivers, that can be used
for lab testing and it depends on proprietary files that come from NXP.

Toradex makes this files available to customers under NDA, please contact your TSE or FAE for information on how to get access
to the necessary files.

Please see the corresponding sections below for details.

Supported SOMS:
- colibri-imx6ull: interface-diversity-sd-sd
- colibri-imx8x:   interface-diversity-pcie-usb
- verdin-imx8mm:   interface-diversity-sd-sd
- verdin-imx8mp:   interface-diversity-sd-uart
- apalis-imx8:     interface-diversity-pcie-usb

# How to install

## Setup layer
The first step is to clone this layer in your own yocto layers directory. This can be done manually or by using repo:

```
$ export LAYER_DIR=<full-path-to-your-toradex-bsp-layer-directory>
$ export BUILD_DIR=<full-path-to-your-toradex-bsp-build-directory>
$ export TORADEX_WIFI_BRANCH=<selected-wifi-branch>
$ cd ${LAYER_DIR}
$ mkdir ../.repo/local_manifests
$ cat > ../.repo/local_manifests/toradex-wifi.xml << EOF
<?xml version="1.0" encoding="UTF-8" ?>
<manifest>
<remote fetch="https://github.com/toradex/" name="toradex-wifi"/>
<project name="meta-toradex-wifi" remote="toradex-wifi" revision="$TORADEX_WIFI_BRANCH" path="layers/meta-toradex-wifi"/>
</manifest>
EOF
$ repo sync meta-toradex-wifi
```

## Decompress the archive with the proprietary drivers downloaded from Toradex:

```
# source your yocto environment file
$ . export
$ cd $BBPATH
$ tar xf proprietary_drivers_nxp_wifi.tar
```

After this, you should have a `wifi-archive` directory on the topdir of your yocto build.

## Enable the layer in your build:

```
$ cd $BBPATH/layers/meta-toradex-wifi
$ ./install_layer.sh
```

The `install_layer.sh` script will setup all the necessary variables to enable the layer and to build and enable the
proprietary drivers by default. Consult this script if you want to know the details of this operation.

## Configure
There are a few configuration options available with this layer. These options are specified in your local.conf or auto.conf file using the MACHINEOVERRIDES variable.

To set the default driver to be used at runtime to the mlan driver, add the following to your config file:

```
MACHINEOVERRIDES =. "default-nxp-proprietary-driver:"
```

To enable manufacturing mode, use the above setting to default to the mlan driver and add the following to your config file:

```
MACHINEOVERRIDES =. "mfg-mode:"
```

## Build

With the above setup, your normal bitbake builds should work and the logic in the layer will set everything else up for you.

```
$ bitbake tdx-reference-minimal-image
```

## Runtime

The toradex-wifi-config recipe will install the /etc/modprobe.d/toradex-wifi-config.conf file with contents similar to the following:

```
# blacklist mlan bt8xxx
# install mlan /bin/false
# install bt8xxx /bin/false

blacklist mwifiex mwifiex_sdio btmrvl btmrvl_sdio
install mwifiex /bin/false
install btmrvl /bin/false
```

To switch between drivers, simply comment out one set of entries, and uncomment the other. Then you will need to reboot.

## Manufacturing Mode

If your build has been configured for manufacturing mode, you will have a binary executable named labtool in the /home/root directory.

# Dependencies

  URI: git://git.toradex.com/meta-toradex-bsp-common
  branch: dunfell-5.x.y
  revision: HEAD

  URI: git://git.openembedded.org/bitbake
  branch: dunfell
  revision: HEAD

  URI: git://git.openembedded.org/openembedded-core
  layers: meta
  branch: dunfell
  revision: HEAD

