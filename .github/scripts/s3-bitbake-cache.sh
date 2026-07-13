#!/usr/bin/env bash
# BitBake cache helpers for oe-core CI.
#
# Stores cache trees as tar.zst (preserves empty dirs for OE git cache).
# Push skips archive+upload when a content fingerprint matches the remote manifest.
# Pull prefers tar.zst and falls back to legacy zip.
#
# Cache types default to: downloads sstate git pnpm electron pip pip-buildenv
# Override with S3_BITBAKE_CACHE_TYPES (comma-separated), e.g. CI skips git:
#   S3_BITBAKE_CACHE_TYPES=downloads,sstate,pnpm,electron,pip,pip-buildenv
set -euo pipefail

DEFAULT_CACHE_TYPES=(downloads sstate git pnpm electron pip pip-buildenv)
CACHE_TYPES=("${DEFAULT_CACHE_TYPES[@]}")
# Set by ensure_tools: zstd | zip
ARCHIVE_FORMAT="${ARCHIVE_FORMAT:-}"

log() {
  echo "$@"
}

resolve_cache_types() {
  local raw="${S3_BITBAKE_CACHE_TYPES:-}"
  local t
  if [[ -z "$raw" ]]; then
    CACHE_TYPES=("${DEFAULT_CACHE_TYPES[@]}")
  else
    CACHE_TYPES=()
    # shellcheck disable=SC2086
    for t in ${raw//,/ }; do
      t="${t#"${t%%[![:space:]]*}"}"
      t="${t%"${t##*[![:space:]]}"}"
      [[ -n "$t" ]] && CACHE_TYPES+=("$t")
    done
  fi
  if [[ ${#CACHE_TYPES[@]} -eq 0 ]]; then
    log "ERROR: S3_BITBAKE_CACHE_TYPES resolved to an empty list" >&2
    return 1
  fi
  log "Using cache types: ${CACHE_TYPES[*]}"
}

ensure_tools() {
  if command -v zstd >/dev/null 2>&1; then
    ARCHIVE_FORMAT=zstd
    log "Using zstd ($(command -v zstd))"
    return 0
  fi

  log "zstd not found; attempting install..."
  if command -v dnf >/dev/null 2>&1; then
    sudo dnf install -y zstd || true
  elif command -v yum >/dev/null 2>&1; then
    sudo yum install -y zstd || true
  elif command -v apt-get >/dev/null 2>&1; then
    sudo apt-get update -qq || true
    sudo DEBIAN_FRONTEND=noninteractive apt-get install -y -qq zstd || true
  fi

  # Refresh command hash in case the package just landed on PATH.
  hash -r 2>/dev/null || true

  if command -v zstd >/dev/null 2>&1; then
    ARCHIVE_FORMAT=zstd
    log "Using zstd after install ($(command -v zstd))"
    return 0
  fi

  if command -v zip >/dev/null 2>&1 && command -v unzip >/dev/null 2>&1; then
    ARCHIVE_FORMAT=zip
    log "WARNING: zstd unavailable; falling back to zip for cache archives"
    return 0
  fi

  log "ERROR: neither zstd nor zip/unzip is available" >&2
  return 1
}

# Fingerprint: sorted path + size for every file, plus empty dirs.
# Prefer GNU find -printf (fast); fall back to a portable path.
fingerprint_dir() {
  local dir="$1"
  if [[ ! -d "$dir" ]]; then
    echo "missing"
    return 0
  fi
  (
    cd "$dir"
    if find . -maxdepth 0 -printf '%p\n' >/dev/null 2>&1; then
      find . -type f -printf 'f %P %s\n' 2>/dev/null
      find . -type d -empty -printf 'd %P\n' 2>/dev/null
    else
      find . -type f -exec stat -c 'f %n %s' {} + 2>/dev/null | sed 's|^\(f\) \./|\1 |'
      find . -type d -empty -print 2>/dev/null | sed 's|^\./||' | sed 's|^|d |'
    fi
  ) | LC_ALL=C sort | sha256sum | awk '{print $1}'
}

archive_path() {
  local cachedir="$1"
  local cachetype="$2"
  if [[ "${ARCHIVE_FORMAT}" == "zip" ]]; then
    echo "${cachedir}/../${cachetype}.zip"
  else
    echo "${cachedir}/../${cachetype}.tar.zst"
  fi
}

create_archive() {
  local src="$1"
  local dest="$2"
  rm -f "$dest"
  if [[ "${ARCHIVE_FORMAT}" == "zip" ]]; then
    # zip from inside the tree so paths match the legacy layout
    (cd "$src" && zip -q -r --symlinks "$dest" .)
  else
    tar -C "$src" -cf - . | zstd -T0 -3 -q -o "$dest"
  fi
}

extract_archive() {
  local archive="$1"
  local dest="$2"
  local format="$3"
  mkdir -p "$dest"
  case "$format" in
    tar.zst)
      zstd -d -T0 -c "$archive" | tar -C "$dest" -xf -
      ;;
    zip)
      unzip -q -u -o "$archive" -d "$dest"
      ;;
    *)
      log "ERROR: unknown archive format ${format}" >&2
      return 1
      ;;
  esac
}

# Returns 0 if object exists.
s3_exists() {
  local uri="$1"
  aws s3api head-object \
    --bucket "$(echo "$uri" | sed -E 's|^s3://([^/]+)/.*|\1|')" \
    --key "$(echo "$uri" | sed -E 's|^s3://[^/]+/||')" \
    >/dev/null 2>&1
}

s3_cp() {
  # Keep errors visible in the job log (do not swallow stderr).
  aws s3 cp --no-progress "$@"
}

download_object() {
  local src="$1"
  local dest="$2"
  local max_attempts=3
  local attempt=1
  local rc=0
  while true; do
    log "Downloading ${src} -> ${dest} (attempt ${attempt}/${max_attempts})"
    if s3_cp "$src" "$dest"; then
      return 0
    fi
    rc=$?
    log "Download failed for ${src} (exit ${rc})"
    if [[ $attempt -ge $max_attempts ]]; then
      return "$rc"
    fi
    log "Retrying in $((attempt * 15))s..."
    sleep $((attempt * 15))
    attempt=$((attempt + 1))
  done
}

pull_all() {
  local s3_prefix="$1"
  local cachedir="$2"
  resolve_cache_types
  ensure_tools

  local -a pids=()
  local -a types_ok=()
  local cachetype
  local workdir
  workdir="$(mktemp -d -t s3-cache-pull-XXXXXX)"
  mkdir -p "$cachedir"

  download_job() {
    local cachetype="$1"
    local outdir="$2"
    local marker="${outdir}/${cachetype}.format"
    local zst_uri="${s3_prefix}/${cachetype}.tar.zst"
    local zip_uri="${s3_prefix}/${cachetype}.zip"
    local archive=""

    if s3_exists "$zst_uri"; then
      archive="${outdir}/${cachetype}.tar.zst"
      if download_object "$zst_uri" "$archive"; then
        echo "tar.zst" >"$marker"
        log "Fetched ${cachetype}.tar.zst ($(du -h "$archive" | cut -f1)B)"
        return 0
      fi
    else
      log "No ${cachetype}.tar.zst in S3; trying legacy zip"
    fi

    if s3_exists "$zip_uri"; then
      archive="${outdir}/${cachetype}.zip"
      if download_object "$zip_uri" "$archive"; then
        echo "zip" >"$marker"
        log "Fetched ${cachetype}.zip ($(du -h "$archive" | cut -f1)B)"
        return 0
      fi
    else
      log "No ${cachetype}.zip in S3 either"
    fi

    log "S3 fetch for ${cachetype} failed; skipping this cache type"
    return 1
  }

  for cachetype in "${CACHE_TYPES[@]}"; do
    download_job "$cachetype" "$workdir" &
    pids+=($!)
  done

  local i=0
  for cachetype in "${CACHE_TYPES[@]}"; do
    if wait "${pids[$i]}"; then
      types_ok+=("$cachetype")
    fi
    i=$((i + 1))
  done

  if [[ ${#types_ok[@]} -eq 0 ]]; then
    log "WARNING: no cache archives downloaded; BitBake will be a cold build"
    rm -rf "$workdir"
    return 0
  fi

  for cachetype in "${types_ok[@]}"; do
    local thiscache="${cachedir}/${cachetype}"
    local marker="${workdir}/${cachetype}.format"
    local format
    format="$(cat "$marker")"
    mkdir -p "$thiscache"
    log "Extracting ${cachetype} (${format}) to ${thiscache}"
    local extract_start extract_end
    extract_start=$(date +%s)
    if [[ "$format" == "tar.zst" ]]; then
      extract_archive "${workdir}/${cachetype}.tar.zst" "$thiscache" tar.zst
      rm -f "${workdir}/${cachetype}.tar.zst"
    else
      extract_archive "${workdir}/${cachetype}.zip" "$thiscache" zip
      rm -f "${workdir}/${cachetype}.zip"
    fi
    extract_end=$(date +%s)
    log "Extracted $(du -h -d 1 "$thiscache" | tail -n 1 | cut -f1)B to ${thiscache} in $((extract_end - extract_start))s"
  done

  rm -rf "$workdir"
}

push_one() {
  local s3_prefix="$1"
  local cachedir="$2"
  local cachetype="$3"
  local thiscache="${cachedir}/${cachetype}"
  local archive
  archive="$(archive_path "$cachedir" "$cachetype")"
  local manifest_remote="${s3_prefix}/${cachetype}.manifest"
  local local_fp remote_fp
  local remote_key_ext="tar.zst"

  if [[ "${ARCHIVE_FORMAT}" == "zip" ]]; then
    remote_key_ext="zip"
  fi

  if [[ ! -d "$thiscache" ]]; then
    log "No local cache directory for ${cachetype}; skipping"
    return 0
  fi

  local file_count
  file_count=$(find "$thiscache" -type f 2>/dev/null | wc -l | tr -d ' ')
  log "Cache ${cachetype}: ${file_count} files, $(du -sh "$thiscache" | cut -f1)"

  if [[ "$file_count" -eq 0 ]]; then
    log "Cache ${cachetype} empty; skipping"
    return 0
  fi

  df -h
  log "Fingerprinting ${cachetype} at ${thiscache}"
  local fp_start fp_end
  fp_start=$(date +%s)
  local_fp="$(fingerprint_dir "$thiscache")"
  fp_end=$(date +%s)
  log "Fingerprint for ${cachetype}: ${local_fp} (computed in $((fp_end - fp_start))s)"

  remote_fp=""
  if remote_fp="$(aws s3 cp --no-progress "$manifest_remote" - 2>/dev/null)"; then
    remote_fp="$(echo -n "$remote_fp" | tr -d '[:space:]')"
    log "Remote fingerprint for ${cachetype}: ${remote_fp}"
  else
    log "No remote manifest for ${cachetype}"
  fi

  if [[ -n "$remote_fp" && "$local_fp" == "$remote_fp" ]]; then
    log "Skipping ${cachetype}: fingerprint unchanged"
    return 0
  fi

  log "Archiving ${cachetype} (${ARCHIVE_FORMAT}) from ${thiscache} to ${archive}"
  local arch_start arch_end
  arch_start=$(date +%s)
  create_archive "$thiscache" "$archive"
  arch_end=$(date +%s)
  log "Archived ${cachetype} in $((arch_end - arch_start))s ($(du -h "$archive" | cut -f1)B)"

  log "Uploading ${cachetype}.${remote_key_ext}"
  local up_start up_end
  up_start=$(date +%s)
  s3_cp "$archive" "${s3_prefix}/${cachetype}.${remote_key_ext}"
  up_end=$(date +%s)
  log "Uploaded $(du -h "$archive" | cut -f1)B in $((up_end - up_start))s"

  echo -n "$local_fp" | s3_cp - "$manifest_remote"
  log "Wrote ${cachetype}.manifest"

  if [[ "$remote_key_ext" == "tar.zst" ]]; then
    # Drop legacy zip once tar.zst is published so the bucket size check stays accurate.
    aws s3 rm "${s3_prefix}/${cachetype}.zip" 2>/dev/null || true
  fi

  rm -f "$archive"
}

push_all() {
  local s3_prefix="$1"
  local cachedir="$2"
  resolve_cache_types
  ensure_tools

  if [[ -d "$cachedir" ]]; then
    sudo chown -R "$(id -u):$(id -g)" "$cachedir" || true
  fi

  local cachetype
  for cachetype in "${CACHE_TYPES[@]}"; do
    push_one "$s3_prefix" "$cachedir" "$cachetype"
  done
}

usage() {
  cat <<EOF
Usage:
  $0 ensure-tools
  $0 pull <s3_prefix> <cachedir>
  $0 push <s3_prefix> <cachedir>

Optional env:
  S3_BITBAKE_CACHE_TYPES   Comma-separated subset of:
                           downloads,sstate,git,pnpm,electron,pip,pip-buildenv
EOF
}

main() {
  local cmd="${1:-}"
  case "$cmd" in
    ensure-tools)
      ensure_tools
      ;;
    pull)
      if [[ $# -ne 3 ]]; then usage; exit 1; fi
      pull_all "$2" "$3"
      ;;
    push)
      if [[ $# -ne 3 ]]; then usage; exit 1; fi
      push_all "$2" "$3"
      ;;
    *)
      usage
      exit 1
      ;;
  esac
}

main "$@"
