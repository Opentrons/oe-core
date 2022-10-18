#!/usr/bin/env bash
set -euo pipefail

# Update all the submodules with their specific needs.

git submodule update --checkout --init
