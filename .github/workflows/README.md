# Build systems for OE-core

This directory contains git workflows for building our openembedded system.

Buidls are done in github workflows. The main workflow that does the build is `./build-ot3-actions.yml`. This workflow runs on self-hosted runners on AWS. It does some custom caching to make the builds not take so long:
- There is a `LOCAL_CACHE` environment variable injected that points to a directory that will be present on subsequent builds on the same runner
- There is an `S3_CACHE_ARN` environment variable that is the ARN of an s3 bucket that can be used for caching

Cache is stored on the S3 bucket as a big zip (since OE git cache needs to store empty directories and S3 doesn't do that on its own). We fetch cache before every build, and update `LOCAL_CACHE` with it. If the build succeeds, we update the zip and write it back.

Build results get sent to an artifact bucket identified by `S3_ARTIFACT_ARN`.

We have to be a little more careful with removing working directories here than in normal github actions.

The main workflow that does the build is triggered only by workflow_dispatch. This can be done manually if you want, but the intended workflow is that another github workflow in some dependent repo will specify the run.
