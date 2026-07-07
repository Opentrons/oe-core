#!/usr/bin/env python3
"""Build JSON payload for the oe-core Flex build Slack Workflow trigger."""

import json
import os


def _display_ref(ref: str) -> str:
    for prefix in ("refs/tags/", "refs/heads/"):
        if ref.startswith(prefix):
            return ref[len(prefix) :]
    return ref


def _headline(status: str) -> str:
    if status in ("deployed", "success"):
        return "Build artifacts deployed"
    if status == "failure":
        return "Build failed"
    if status == "cancelled":
        return "Build cancelled"
    return "Build update"


def _status_label(status: str) -> str:
    if status in ("deployed", "success"):
        return "deployed"
    return status


def main() -> None:
    monorepo_ref = os.environ.get("INPUT_MONOREPO_REF", "")
    status = os.environ.get("INPUT_STATUS", "")

    monorepo_display = _display_ref(monorepo_ref)
    if monorepo_ref.startswith("refs/tags/"):
        ref_type = "tag"
        tag = monorepo_display
    else:
        ref_type = "branch"
        tag = "None"

    console_url = os.environ.get("INPUT_CONSOLE_URL", "")
    if console_url and not console_url.endswith("/"):
        console_url = f"{console_url}/"

    payload = {
        "tag": tag,
        "headline": _headline(status),
        "status": _status_label(status),
        "s3-url": console_url,
        "type": ref_type,
        "reflike": _display_ref(os.environ.get("INPUT_OE_CORE_REF", "")),
        "monorepo-reflike": monorepo_display,
        "firmware-reflike": _display_ref(os.environ.get("INPUT_FIRMWARE_REF", "")),
        "full-image": os.environ.get("INPUT_FULLIMAGE_URL", ""),
        "system-update": os.environ.get("INPUT_SYSTEM_URL", ""),
        "version-file": os.environ.get("INPUT_VERSION_FILE_URL", ""),
        "release-notes": os.environ.get("INPUT_RELEASE_NOTES_FILE_URL", ""),
    }
    print(json.dumps(payload))


if __name__ == "__main__":
    main()
