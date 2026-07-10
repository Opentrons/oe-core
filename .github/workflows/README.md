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
- Cache is stored on the S3 bucket as `downloads` / `sstate` / `git` **`tar.zst`** archives (tar preserves empty directories needed by the OE git cache). Legacy `.zip` objects are still readable on pull during migration.
- Pull downloads the three archives **in parallel**, then extracts them sequentially.
- Push fingerprints each cache tree (`path` + `size`, plus empty dirs). If the fingerprint matches the remote `*.manifest`, that cache type is **skipped** (no re-archive / re-upload).
- Push uses multi-threaded `zstd` compression. After a successful `tar.zst` upload, the legacy `.zip` for that type is deleted from the bucket.
- Build results get sent to an artifact bucket identified by `S3_ARTIFACT_ARN`.
- We have to be a little more careful with removing working directories here than in normal github actions.

Helpers live in [`.github/scripts/s3-bitbake-cache.sh`](../scripts/s3-bitbake-cache.sh).

## Docker image BuildKit cache (ECR)

The `run-build` job builds the oe-core `Dockerfile` on each ephemeral runner. When configured, BuildKit stores reusable image layers in ECR so repeated builds skip the expensive `apt-get` stack when `Dockerfile` is unchanged.

Set these repository variables (per AWS account / infra stage):

| Variable | Example value |
| --- | --- |
| `OT3_OE_DOCKER_ECR_REPOSITORY_PROD` | `123456789.dkr.ecr.us-east-2.amazonaws.com/ot3-oe-ci-image` |
| `OT3_OE_DOCKER_ECR_REPOSITORY_DEV` | `123456789.dkr.ecr.us-east-2.amazonaws.com/ot3-oe-ci-image` |

Cache tags look like `buildcache-<dockerfile-sha12>` on that repository. The workflow creates the ECR repository if it does not exist.

The `ROBOT_STACK_AWS_OIDC_ROLE_ARN_*` role used for the build needs ECR permissions: `ecr:GetAuthorizationToken`, `ecr:CreateRepository`, `ecr:DescribeRepositories`, `ecr:BatchCheckLayerAvailability`, `ecr:GetDownloadUrlForLayer`, `ecr:BatchGetImage`, `ecr:PutImage`, `ecr:InitiateLayerUpload`, `ecr:UploadLayerPart`, `ecr:CompleteLayerUpload`.

If the variables are unset, CI falls back to a plain `docker build` with no layer cache.
