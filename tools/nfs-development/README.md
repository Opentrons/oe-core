# OT-3 NFS development

When you're messing around with an OT-3 test system, sometimes you don't want to do a whole big update dance, and just want to change some files. One way to do that is to configure the Verdin SOM to boot off a network. 

That, in turn, is a bit of a pain, but what's here can make it less painful (hopefully).

The way this works is a netboot pipeline based on configuring a u-boot bootloader that lives on the toradex verdin's internal storage. There's an image here that does that, and so initial setup for the system requires running `./setup.sh`. 

Once the proper bootloader is on the system, it will be configured to talk to a dhcp server to figure out where it can boot from. The DHCP server will point it to a TFTP share on the local network containing the kernel to boot and any other boot time configurations (such as device trees). U-boot fetches the kernel, and runs it with configuration to look to NFS to get its root filesystem. After that, it'll be up and running based on files just on your local machine, and you won't have to change anything on the toradex (probably avoid editing those shared files on your machine, though - it interacts poorly with NFS caching).


## setup

Get the verdin running easyinstaller, either because you just unboxed it or through the [recovery mode](https://developer.toradex.com/knowledge-base/imx-recovery-mode#Verdin_SoM_Family) procedure. Then EXTRACT the image checked in here as `initial-setup.tar.gz` to an sd card, put the sd card in the toradex, and it'll install.

On a linux machine or vm, install docker and lxc (if on ubuntu)


## general use

The actual server system consists of a dockerfile that ties together the three things we need for a full from-uboot netboot:

1. A dhcp server to provide a static ip lease to u-boot, and inform it of the availability of netboot and the location of the kernel
2. A tftp server to actually give u-boot the kernel
3. An nfs server to provide the rootfs

The docker container is provided basically for convenience in installing most of the dependencies, but these are things that fundamentally reach out of the container and require some host machine (and real-world) setup. For instance,
- You should really connect the verdin directly to an ethernet adapter on your host machine. This system absolutely relies on a DHCP server running in the container, which means it's a _really really bad idea_ to try and do this over an actual network that other people actually use. It will break everything. Connect this directly. That's what the iface name in the environment file is for
- When you run the script that builds and executes the docker container, you're going to need `sudo`, because the container has to interact with the kernel to actually serve nfs - it's run by the kernel. That means the script runs the container with `CAP_SYS_ADMIN`.
- For the same reason, the host kernel will have to be running the nfs server modules. The container should take care of it, but be aware.

### setup steps
- Copy ot3-bootserver.env.template to ot3-bootserver.env
- Identify the network interface that you want to give to dhcp by name. This is the name that comes up in ifconfig. Put this name in ot3-bootserver.env's IFACE variable.
- Find the verdin's MAC address. This is toradex's prefix 00:14:2D and then the serial in hex. Per [their docs](https://developer.toradex.com/knowledge-base/mac-address), for instance serial 002-0004380 is MAC 00:14:2D:00:11:1C, and serial  02226142 is MAC 00:14:2D:21:F7:DE. Put this mac in ot3-bootserver.env's VERDIN_MAC variable.
- Figure out the networking settings you want to use. DHCPD is going to create a second network, and your machine could get confused if you give it the same IP range as the one you're connected to for the internet. The two common LAN IP ranges are 192.168.0.0/16 and 10.10.0.0/16; pick the one you aren't using and set that as the DHCP_ADDRESS_BASE value.
- Make sure that the interface you're giving dhcpd is set to a static IP of address 1 in the IP range you gave, either 192.168.0.1 or 10.10.0.1.
- Install the `netboot` image on the verdin using easyinstall

### run steps
- run `./start.sh`. you can optionally provide a path to an image, like `./start.sh /path/to/opentrons-image.tar`, in which case the image will be automatically extracted (including recursively) to the right place. If you don't provide this, whatever's in the `provide/` directory just gets provided, including possibly nothing if you haven't set anything up. If you want to set it up manually, extract the contents of the root filesystem to `provide/root/`, and put the `Image.gz` in `provide/boot/`.
