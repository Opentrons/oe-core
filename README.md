# OE-Core

This is the home for the openembedded build.

It contains build tooling, git submodules for key repositories required for the build, and a small amount of build-wide configuration.

The primary purpose of this repo is to be the root of the configuration tree that determines the version of the components of the build - at the end of the day, the versions of the submodules of this repo determine what is built.

As such, the submodules of this repo should always point to something more restrictive than the version in which they live.

For instance, a tagged version of this subrepo _must_ have every submodule pointing to a specific commit or tag rather than tracking a remote branch. The `main` branch of this repo should have those submodules pointing to either specific commits or tags, or possibly a `main` equivalent, because it should be releasable. Working branches for PRs or a moving `edge` branch may point at branches in the submodules.
