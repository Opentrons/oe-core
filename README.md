# OE-Core

This is the home for the openembedded build.

It contains build tooling, git submodules for key repositories required for the build, and a small amount of build-wide configuration.

The primary purpose of this repo is to be the root of the configuration tree that determines the version of the components of the build - at the end of the day, the versions of the submodules of this repo determine what is built.

As such, the submodules of this repo should always point to something more restrictive than the version in which they live.

For instance, a tagged version of this subrepo _must_ have every submodule pointing to a specific commit or tag rather than tracking a remote branch. The `main` branch of this repo should have those submodules pointing to either specific commits or tags, or possibly a `main` equivalent, because it should be releasable. Working branches for PRs or a moving `edge` branch may point at branches in the submodules.

While git (as of 2.22) supports having a submodule that tracks an upstream branch, it does this by changing the way that you call `git submodule update` and this applies to _all_ submodules, even those you don't want to track an upstream. For that reason, there's an `update.sh` script that just does the right submodule commands. 

**MAKE SURE TO RUN `./update.sh` AFTER YOU SWITCH BRANCHES AND AFTER CLONING.**

To change what a recipe checks out, cd into that recipe and change the branch or the commit.


## Building

See [.github/workflows](.github/workflows) for details on how to trigger the automated builds.

Do not try to build this on anything other than a linux machine that is extremely beefy. It requires docker, which will make it incredibly slow on osx, and uses bind mounts, which will make it incredibly incredibly slow on osx, and it uses bash and default paths outside the container, which will make it not work on windows. Try and use something that has like 6C/12T and at least 32GiB RAM. Or rely on the automated builds.

### ot3Image.sh
Once you have all that, you should be able to run `./ot3Image.sh` and it will build you an image. Positional arguments to `./ot3Image.sh` will be passed (eventually) to bitbake, so you can run for instance `./ot3Image.sh --setscene-only opentrons-ot3-image` and it will run `bitbake --setscene-only opentrons-ot3-image`.

### start.sh
If you don't want to use docker and have installed everything the docker image requires, you can also run the inner execution script, `start.sh`, directly. It requires the path to `oe-core` (this is used inside the docker container) and subsequent arguments are optionally passed to bitbake. For instance from `oe-core` you would run `start.sh .`, and to run the same command as the docker example above you
would run `start.sh . --setscene-only opentrons-ot3-image`.

### running commands directly

If you also don't want to use `start.sh` (please consider adding the capability to do whatever you want to do to `start.sh`, the docker container, and `ot3Image.sh`) you can run the bitbake commands directly:

`BITBAKEDIR=$(pwd)/tools/bitbake . ./layers/openembedded-core/oe-init-build-env`

You'll get moved to `build`. Then you can run `bitbake`. To check recipe errors you can try `bitbake --setscene-only RECIPENAME`.

## Updating from upstream

If you want to update to the latest upstream release from toradex, use `scripts/set-refs-from-tdx-manifest.py`. Run it with `uv` to install its dependencies.
