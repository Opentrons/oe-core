# OE-Core

This is the home for the openembedded build.

It contains build tooling, git submodules for key repositories required for the build, and a small amount of build-wide configuration.

The primary purpose of this repo is to be the root of the configuration tree that determines the version of the components of the build - at the end of the day, the versions of the submodules of this repo determine what is built.

As such, the submodules of this repo should always point to something more restrictive than the version in which they live.

For instance, a tagged version of this subrepo _must_ have every submodule pointing to a specific commit or tag rather than tracking a remote branch. The `main` branch of this repo should have those submodules pointing to either specific commits or tags, or possibly a `main` equivalent, because it should be releasable. Working branches for PRs or a moving `edge` branch may point at branches in the submodules.

While git (as of 2.22) supports having a submodule that tracks an upstream branch, it does this by changing the way that you call `git submodule update` and this applies to _all_ submodules, even those you don't want to track an upstream. For that reason, there's an `update.sh` script that just does the right submodule commands. 

**MAKE SURE TO RUN `./update` AFTER YOU SWITCH BRANCHES AND AFTER CLONING.**

To change what a recipe checks out, cd into that recipe and change the branch or the commit.


## Building

Do not try to build this on anything other than a linux machine that is extremely beefy. It requires docker, which will make it incredibly slow on osx, and uses bind mounts, which will make it incredibly incredibly slow on osx, and it uses bash and default paths outside the container, which will make it not work on windows. Try and use something that has like 6C/12T and at least 32GiB RAM. Or rely on the automated builds.

Once you have all that, you should be able to run `./ot3image.sh` and it will build you an image.

You can also manually run some of the steps in `./ot3image.sh` yourself if there are some things you want to check. First, source the openembedded setup script:

`BITBAKEDIR=$(pwd)/tools/bitbake . ./layers/openembedded-core/oe-init-build-env`

You'll get moved to `build`. Then you can run `bitbake`. To check recipe errors you can try `bitbake --setscene-only RECIPENAME`. You can also just change the target in `start.sh` and run `ot3image.sh`.

## Running

The images built by this repo can be installed with easyinstall on a toradex. That requires putting the verdin in recovery mode with easyinstall running from ram via libuuu and then putting the image on some removable storage, inserting it into the verdin, and writing it to the mmc.

You can also boot the verdin from an ethernet connection directly to a host machine; see tools/nfs-development for more.
