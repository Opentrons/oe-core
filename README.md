# OE-Core

This is the home for the openembedded build.

It contains build tooling, git submodules for key repositories required for the build, and a small amount of build-wide configuration.

The primary purpose of this repo is to be the root of the configuration tree that determines the version of the components of the build - at the end of the day, the versions of the submodules of this repo determine what is built.

As such, the submodules of this repo should always point to something more restrictive than the version in which they live.

For instance, a tagged version of this subrepo _must_ have every submodule pointing to a specific commit or tag rather than tracking a remote branch. The `main` branch of this repo should have those submodules pointing to either specific commits or tags, or possibly a `main` equivalent, because it should be releasable. Working branches for PRs or a moving `edge` branch may point at branches in the submodules.

MAKE SURE TO RUN `git submodule update --init` AFTER YOU SWITCH BRANCHES AND AFTER CLONING.

## Building

Do not try to build this on anything other than a linux machine that is extremely beefy. It requires docker, which will make it incredibly slow on osx, and uses bind mounts, which will make it incredibly incredibly slow on osx, and it uses bash and default paths outside the container, which will make it not work on windows. Try and use something that has like 6C/12T and at least 32GiB RAM. Or rely on the automated builds.

Once you have all that, you should be able to run `./ot3image.sh` and it will build you an image.
