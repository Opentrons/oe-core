# Build systems for oe-core

This directory contains git workflows for building our openembedded system.

The main workflow that does the build is [build-ot3-actions.yml](build-ot3-actions.yml). This workflow runs on self-hosted runners on AWS. Custom caching is used to reduce build times. [build-ot3-actions.yml](build-ot3-actions.yml) is triggered **only** by workflow_dispatch.

## Triggers

> [!NOTE]
> Builds are costly and take at least an hour. We want to avoid running them unnecessarily. Soon we will move to ephemeral runners that are dynamically requested, but currently we are limited to one run per "channel".

Builds are triggered:

- When something in this repository itself changes. This is controlled by a workflow in this repository: [build-branches.yml](build-branches.yml).
- When you [manually start a build from this repository][manually-run]. This lets you select an arbitrary combination of `oe-core`, `opentrons`, and `ot3-firmware` refs, and is useful for testing a synchronized change.
- When something in <https://github.com/Opentrons/opentrons> changes. This is controlled by a workflow in that repository. The most common triggers are pushes to its `edge` branch and tagging of releases. This triggers the majority of builds.
- When something in <https://github.com/Opentrons/ot3-firmware> changes. This is controlled by a workflow in that repository, watching for pushes to its `main` branch.

[manually-run]: https://docs.github.com/en/actions/how-tos/manage-workflow-runs/manually-run-a-workflow#running-a-workflow

## Nuances of caching

- There is a `LOCAL_CACHE` environment variable injected that points to a directory that will be present on subsequent builds on the same runner.
- There is an `S3_CACHE_ARN` environment variable that is the ARN of an s3 bucket that can be used for caching.
- BitBake cache trees (`downloads` / `sstate` / `git`) are stored on S3 as **`tar.zst`** plus a **`.manifest`** fingerprint. Helper: [`.github/scripts/s3-bitbake-cache.sh`](../scripts/s3-bitbake-cache.sh).
- **Pull:** download each `.tar.zst` that exists (in parallel), then extract sequentially. Missing objects mean a cold/partial cache.
- **Push:** fingerprint each tree (`path` + `size`, plus empty dirs). If it matches the remote `.manifest`, skip; otherwise archive with `zstd` and upload `.tar.zst` + `.manifest`.
- **zstd** must already be on the ephemeral runner image (the script prints `zstd --version` or fails; it does not install packages).
- First run after this format lands expects no hit; the following push seeds the new objects.
- Build results go to an artifact bucket identified by `S3_ARTIFACT_ARN`.
- We have to be a little more careful with removing working directories here than in normal github actions.
