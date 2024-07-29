# Build systems for oe-core

This directory contains git workflows for building our openembedded system.

The main workflow that does the build is [build-ot3-actions.yml](build-ot3-actions.yml). This workflow runs on self-hosted runners on AWS. Custom caching is used to reduce build times. [build-ot3-actions.yml](build-ot3-actions.yml) is triggered **only** by workflow_dispatch.

## Builds, Triggers, and Concurrency

> Builds are costly and take at least an hour. We want to avoid running them unnecessarily. Soon we will move to ephemeral runners that are dynamically requested, but currently we use always on runners in EC2. This limits us to one run per "channel". Concurrency allows us to avoid stacking up builds when multiple merges or commits happen in quick succession. A concurrency group containing tags should only be initiated during the release process.

- Builds triggered by this repository itself are controlled by [build-branches.yml](build-branches.yml)
- <https://github.com/Opentrons/opentrons> triggers the majority of builds. The most common triggers are pushes to its `edge` branch and tagging of releases.
  - we expect the concurrency group `edge----` to get cancelled frequently, but this makes sense as we don't need a build for every commit to edge.  Instead we will have a build finish every commit to `edge` not followed by another commit within ~1.5 hours.
- <https://github.com/Opentrons/ot3-firmware> triggers builds on pushes to its `main` branch.

## Nuances of caching

- There is a `LOCAL_CACHE` environment variable injected that points to a directory that will be present on subsequent builds on the same runner.
- There is an `S3_CACHE_ARN` environment variable that is the ARN of an s3 bucket that can be used for caching.
- Cache is stored on the S3 bucket as a big zip (since OE git cache needs to store empty directories and S3 doesn't do that on its own). We fetch cache before every build, and update `LOCAL_CACHE` with it. If the build succeeds, we update the zip and write it back.
- Build results get sent to an artifact bucket identified by `S3_ARTIFACT_ARN`.
- We have to be a little more careful with removing working directories here than in normal github actions.
