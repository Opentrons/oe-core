#!/usr/bin/env bash
set -euo pipefail

# Update all the submodules with their specific needs.

git submodule update --checkout --init
if [ -z "${GITHUB_REF+x}" ]; then
   echo "CI variables detected, setting opentrons-controlled repos as specified"
   git submodule set-branch -b ${GITHUB_REF#/refs/heads} layers/meta-opentrons
fi
git submodule update --checkout --remote layers/meta-opentrons
