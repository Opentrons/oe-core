#!/usr/bin/env bash
# BitBake cache helpers for oe-core CI.
#
# Stores downloads/sstate/git as tar.zst (preserves empty dirs for OE git cache).
# Push skips archive+upload when a content fingerprint matches the remote manifest.
set -euo pipefail

CACHE_TYPES=(downloads sstate git)

ensure_tools() {
  if command -v zstd >/dev/null 2>&1; then
    return 0
  fi
  echo "Installing zstd for BitBake cache archives..."
  if command -v apt-get >/dev/null 2>&1; then
    sudo apt-get update -qq
    sudo DEBIAN_FRONTEND=noninteractive apt-get install -y -qq zstd
  elif command -v dnf >/dev/null 2>&1; then
    sudo dnf install -y -q zstd
  elif command -v yum >/dev/null 2>&1; then
    sudo yum install -y -q zstd
  else
    echo "zstd is required but could not be installed" >&2
    return 1
  fi
  command -v zstd >/dev/null 2>&1
}

# Fingerprint: sorted path + size for every file, plus empty dirs.
# Omit mtime so incidental BitBake touches do not force a full re-upload.
# Empty directories matter for OE git cache.
fingerprint_dir() {
  local dir="$1"
  if [[ ! -d "$dir" ]]; then
    echo "missing"
    return 0
  fi
  (
    cd "$dir"
    find . -type f -printf 'f %P %s\n' 2>/dev/null
    find . -type d -empty -printf 'd %P\n' 2>/dev/null
  ) | LC_ALL=C sort | sha256sum | awk '{print $1}'
}

archive_path() {
  local cachedir="$1"
  local cachetype="$2"
  echo "${cachedir}/../${cachetype}.tar.zst"
}

legacy_zip_path() {
  local cachedir="$1"
  local cachetype="$2"
  echo "${cachedir}/../${cachetype}.zip"
}

create_archive() {
  local src="$1"
  local dest="$2"
  rm -f "$dest"
  # Multi-threaded zstd; tar preserves empty directories (unlike plain S3 sync).
  tar -C "$src" -cf - . | zstd -T0 -3 -q -o "$dest"
}

extract_tar_zst() {
  local archive="$1"
  local dest="$2"
  mkdir -p "$dest"
  zstd -d -T0 -c "$archive" | tar -C "$dest" -xf -
}

extract_zip() {
  local archive="$1"
  local dest="$2"
  mkdir -p "$dest"
  unzip -q -u -o "$archive" -d "$dest"
}

s3_cp() {
  aws s3 cp --no-progress "$@"
}

download_with_retries() {
  local src="$1"
  local dest="$2"
  local elapsed_file="${3:-elapsed}"
  local max_attempts=3
  local attempt=1
  while true; do
    if TIME="%E" time s3_cp "$src" "$dest" 2>"$elapsed_file"; then
      return 0
    fi
    if [[ $attempt -ge $max_attempts ]]; then
      return 1
    fi
    echo "S3 fetch attempt ${attempt} failed, retrying in $((attempt * 15))s..."
    sleep $((attempt * 15))
    attempt=$((attempt + 1))
  done
}

pull_all() {
  local s3_prefix="$1"
  local cachedir="$2"
  ensure_tools

  # Parallel downloads, then sequential extracts, to speed network without
  # holding three full extracted trees + three archives at once.
  local -a pids=()
  local -a types_ok=()
  local cachetype
  local workdir
  workdir="$(mktemp -d -t s3-cache-pull-XXXXXX)"

  download_job() {
    local cachetype="$1"
    local outdir="$2"
    local archive="${outdir}/${cachetype}.tar.zst"
    local legacy="${outdir}/${cachetype}.zip"
    local marker="${outdir}/${cachetype}.format"
    local elapsed_file="${outdir}/${cachetype}.elapsed"
    if download_with_retries "${s3_prefix}/${cachetype}.tar.zst" "$archive" "$elapsed_file"; then
      echo "tar.zst" >"$marker"
      echo "Fetched ${cachetype}.tar.zst ($(du -h "$archive" | cut -f1)B) in $(cat "$elapsed_file")"
      return 0
    fi
    if download_with_retries "${s3_prefix}/${cachetype}.zip" "$legacy" "$elapsed_file"; then
      echo "zip" >"$marker"
      echo "Fetched ${cachetype}.zip ($(du -h "$legacy" | cut -f1)B) in $(cat "$elapsed_file")"
      return 0
    fi
    echo "S3 fetch for ${cachetype} failed after retries, skipping"
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

  for cachetype in "${types_ok[@]}"; do
    local thiscache="${cachedir}/${cachetype}"
    local marker="${workdir}/${cachetype}.format"
    local format
    local elapsed_file="${workdir}/${cachetype}.extract.elapsed"
    format="$(cat "$marker")"
    mkdir -p "$thiscache"
    echo "Extracting ${cachetype} (${format}) to ${thiscache}"
    if [[ "$format" == "tar.zst" ]]; then
      TIME="%E" time extract_tar_zst "${workdir}/${cachetype}.tar.zst" "$thiscache" 2>"$elapsed_file"
      rm -f "${workdir}/${cachetype}.tar.zst"
    else
      TIME="%E" time extract_zip "${workdir}/${cachetype}.zip" "$thiscache" 2>"$elapsed_file"
      rm -f "${workdir}/${cachetype}.zip"
    fi
    echo "Extracted $(du -h -d 1 "$thiscache" | tail -n 1 | cut -f1)B to ${thiscache} in $(cat "$elapsed_file")"
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

  if [[ ! -d "$thiscache" ]]; then
    echo "No local cache directory for ${cachetype}; skipping"
    return 0
  fi

  local elapsed_file
  elapsed_file="$(mktemp)"
  df -h
  echo "Fingerprinting ${cachetype} at ${thiscache}"
  local fp_start fp_end
  fp_start=$(date +%s)
  local_fp="$(fingerprint_dir "$thiscache")"
  fp_end=$(date +%s)
  echo "Fingerprint for ${cachetype}: ${local_fp} (computed in $((fp_end - fp_start))s)"

  remote_fp=""
  if remote_fp="$(aws s3 cp --no-progress "$manifest_remote" - 2>/dev/null)"; then
    remote_fp="$(echo -n "$remote_fp" | tr -d '[:space:]')"
    echo "Remote fingerprint for ${cachetype}: ${remote_fp}"
  else
    echo "No remote manifest for ${cachetype}"
  fi

  if [[ -n "$remote_fp" && "$local_fp" == "$remote_fp" ]]; then
    echo "Skipping ${cachetype}: fingerprint unchanged"
    rm -f "$elapsed_file"
    return 0
  fi

  echo "Archiving ${cachetype} from ${thiscache} to ${archive}"
  TIME="%E" time create_archive "$thiscache" "$archive" 2>"$elapsed_file"
  echo "Archived ${cachetype} in $(cat "$elapsed_file") ($(du -h "$archive" | cut -f1)B)"

  echo "Uploading ${cachetype}.tar.zst"
  TIME="%E" time s3_cp "$archive" "${s3_prefix}/${cachetype}.tar.zst" 2>"$elapsed_file"
  echo "Uploaded $(du -h "$archive" | cut -f1)B in $(cat "$elapsed_file")"

  echo -n "$local_fp" | s3_cp - "$manifest_remote"
  echo "Wrote ${cachetype}.manifest"

  # Drop legacy zip once tar.zst is published so the bucket size check stays accurate.
  aws s3 rm "${s3_prefix}/${cachetype}.zip" 2>/dev/null || true

  rm -f "$archive" "$elapsed_file"
}

push_all() {
  local s3_prefix="$1"
  local cachedir="$2"
  ensure_tools

  if [[ -d "$cachedir" ]]; then
    sudo chown -R "$(id -u):$(id -g)" "$cachedir"
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
