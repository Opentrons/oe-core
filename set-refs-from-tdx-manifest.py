#!/usr/bin/env python3
# /// script
# requires-python = ">=3.10"
# dependencies = [
#     "defusedxml",
# ]
# ///

"""
Update the submodules in this repository from a git ref of git.toradex.com/toradex-manifest.git

git.toradex.com/toradex-manifest.git is a google repotool manifest for an openembedded layer set, and is how toradex
distributes their official versions of their openembedded tree. When they release a new version, they do it by
pushing a branch there. This tool updates our submodule-style openembedded tree from the manifests there (among other
things).
"""

from typing import Iterator, Any, Iterable
from dataclasses import dataclass
import logging
import argparse
import sys
import traceback
import contextlib
from shutil import rmtree
import subprocess
import tempfile
from itertools import chain
from pathlib import Path
from xml.etree.ElementTree import ElementTree

try:
    from defusedxml import ElementTree as ET
except ImportError:
    sys.stderr.write(
        "This script requires https://pypi.org/project/defusedxml/. Run pip install defusedxml or run this script with uv.\n"
    )
    raise
    sys.exit(-1)

LOG = logging.getLogger()

_MANIFEST_REPO = "http://git.toradex.com/toradex-manifest.git"

_REPO_PATH_OVERRIDES = {"tools/bitbake": "layers/openembedded-core/bitbake"}


@dataclass
class SubmoduleSpec:
    tracking: str | None
    sha: str
    name: str
    path: str
    url: str


def _spec_from_lines(git_lines: str) -> Iterator[SubmoduleSpec]:
    for git_line in git_lines:
        if not git_line:
            continue
        flags = git_line[:2]
        content = git_line[2:]
        sha, path, describe = content.split(" ")
        spec = SubmoduleSpec(tracking=None, sha=sha, name=path, path=path, url="")
        LOG.debug(f"submodule: spec {spec} from line {git_line}")
        yield spec


@dataclass
class GitmodulesEntry:
    name: str
    path: str
    url: str
    branch: str | None


def _is_gitmodule_header(line: str) -> bool:
    return line.startswith("[submodule")


def _parse_one_gm_payload_line(
    current_line: str, entry: GitmodulesEntry
) -> GitmodulesEntry:
    name, _, value = current_line.strip().split(" ")
    if name == "path":
        entry.path = value
        LOG.debug(f"parsed url {value} from line {current_line}")
        return entry
    if name == "url":
        entry.url = value
        LOG.debug(f"parsed url {value} from line {current_line}")
        return entry
    if name == "branch":
        entry.branch = value
        LOG.debug(f"parsed branch {value} from line {current_line}")
        return entry
    LOG.warning(
        f"Not parsing gitmodule line {current_line} in context of {entry}, may need to update script"
    )


def parse_one_gitmodule(
    current_line: str, rest_lines: Iterable[str]
) -> tuple[str, Iterable[str], GitmodulesEntry]:
    while not _is_gitmodule_header(current_line):
        current_line = next(rest_lines)
    _, pathplus = current_line.split(" ")
    name = pathplus[1:-2]
    entry = GitmodulesEntry(name=name, path="", url="", branch=None)
    current_line = next(rest_lines)
    try:
        while not _is_gitmodule_header(current_line):
            entry = _parse_one_gm_payload_line(current_line, entry)
            current_line = next(rest_lines)
    finally:
        return (current_line, rest_lines, entry)


def parse_gitmodules(gitmodules_content: str) -> Iterator[GitmodulesEntry]:
    gitmodules_lines = iter(gitmodules_content.strip().split("\n"))
    current_line = next(gitmodules_lines)
    while True:
        try:
            current_line, gitmodules_lines, entry = parse_one_gitmodule(
                current_line, gitmodules_lines
            )
            LOG.debug(f"parsed {entry} from .gitmodules")
            yield entry
        except StopIteration:
            return


def augment_specs_with_gitmodules(
    specs: Iterable[SubmoduleSpec], repo_path: Path
) -> Iterator[SubmoduleSpec]:
    gitmodules_path = repo_path / ".gitmodules"
    LOG.debug(f"reading .gitmodules from {gitmodules_path}")
    gitmodules_file = gitmodules_path.read_text()
    LOG.debug(f"gitmodules is {gitmodules_file}")
    for entry in parse_gitmodules(gitmodules_file):
        for spec in specs:
            if spec.path == entry.path:
                LOG.debug(
                    f"git submodule result {spec} matches .gitmodules entry {entry}"
                )
                spec.name = entry.name
                spec.url = entry.url
                spec.tracking = entry.branch
                yield spec
                break


def parse_current_repo(repo_root: Path) -> list[SubmoduleSpec]:
    LOG.info(f"find current submodule config from repo {repo_root}")
    status_proc = git(["submodule", "status"], stdout=subprocess.PIPE)
    lines = status_proc.stdout.strip().split("\n")
    LOG.debug(status_proc.stdout)
    specs = list(_spec_from_lines(lines))
    specs = list(augment_specs_with_gitmodules(specs, repo_root))
    return specs


def get_all_child_manifests(
    root_manifest: ElementTree, manifest_tree_root: Path
) -> Iterator[ElementTree]:
    for include in root_manifest.getroot().findall("include"):
        LOG.debug(f"including manifest {include.get('name')}")
        child = ET.parse(manifest_tree_root / include.get("name"))
        yield child
        yield from get_all_child_manifests(child, manifest_tree_root)


def merge_manifest_tree(et: ElementTree, manifest_tree_root: Path) -> ElementTree:
    for child_manifest in get_all_child_manifests(et, manifest_tree_root):
        new_nodes = list(child_manifest.getroot().iter())
        LOG.debug(f"extending manifest with {new_nodes}")
        et.getroot().extend(new_nodes)
    return et


def repos_from_merged_manifest(et: ElementTree) -> dict[str, str]:
    repos = {
        repo.get("name"): repo.get("fetch") for repo in et.getroot().findall("remote")
    }
    LOG.debug(f"found manifest repos {repos}")
    return repos


def submodules_from_merged_manifest(
    et: ElementTree,
) -> Iterator[SubmoduleSpec]:
    repos = repos_from_merged_manifest(et)
    for project in et.getroot().findall("project"):
        spec = SubmoduleSpec(
            tracking=project.get("upstream", None),
            sha=project.get("revision"),
            name=project.get("name"),
            path=project.get("path"),
            url=repos[project.get("remote")],
        )
        LOG.info(f"Found manifest submodule {spec}")
        LOG.debug(f"from manifest project {project}")
        yield spec


def parse_manifest_tree(
    manifest_root_dir: Path, manifest_file: Path
) -> Iterator[SubmoduleSpec]:
    manifest_tree = ET.parse(manifest_root_dir / manifest_file)
    LOG.debug(f"base manifest tree: {list(manifest_tree.getroot().iter())}")
    manifest_merged = merge_manifest_tree(manifest_tree, manifest_root_dir)
    LOG.debug(f"merged manifest tree: {list(manifest_tree.getroot().iter())}")
    return list(submodules_from_merged_manifest(manifest_merged))


def quiet_proc(quiet: bool) -> dict[str, Any]:
    if not quiet:
        return {}
    return {"stderr": subprocess.STDOUT}


def git(args: list[str], **kwargs: str) -> subprocess.CompletedProcess:
    result = subprocess.run(["git"] + args, text=True, **kwargs)
    result.check_returncode()
    return result


@contextlib.contextmanager
def provide_dir(working_dir: str | None) -> Iterator[Path]:
    if working_dir:
        working = Path(working_dir)
        working.mkdir(parents=True, exist_ok=True)
        yield working
    else:
        with tempfile.TemporaryDirectory() as td:
            yield Path(td)


def ensure_manifest_dir(working_dir: Path) -> Path:
    LOG.info(f"clearing manifest path {working_dir}")
    for path in working_dir.iterdir():
        LOG.debug(f"removing {path}")
        rmtree(path)
    manifest_path = working_dir / "toradex-manifest"
    LOG.debug(f"ready to clone manifest to {manifest_path}")
    return manifest_path


def get_manifest_repo(
    manifest_target_path: Path, manifest_repo: str, quiet: bool
) -> None:
    LOG.info(f"cloning manifest {manifest_repo} to {manifest_target_path}")
    git_results = git(
        ["clone", manifest_repo, manifest_target_path],
        stdout=subprocess.PIPE,
        **quiet_proc(quiet),
    )
    LOG.debug(git_results.stdout)
    LOG.info("done cloning")


def checkout_manifest_ref(
    manifest_target_path: Path, reflike: str, quiet: bool
) -> None:
    LOG.info(f"checking out {reflike} in {manifest_target_path}")
    checkout_results = git(
        ["checkout", reflike],
        cwd=manifest_target_path,
        stdout=subprocess.PIPE,
        **quiet_proc(quiet),
    )
    LOG.debug(checkout_results.stdout)
    LOG.info("done checking out")


def prep_manifest_repo(
    working_dir: Path, manifest_repo: str, reflike: str, quiet: bool
) -> Path:
    manifest_path = ensure_manifest_dir(working_dir)
    get_manifest_repo(manifest_path, manifest_repo, quiet)
    checkout_manifest_ref(manifest_path, reflike, quiet)
    return manifest_path


def match_manifest_to_repo(
    manifest_modules: list[SubmoduleSpec], repo_module: SubmoduleSpec
) -> Iterator[tuple[SubmoduleSpec, SubmoduleSpec]]:
    for manifest in manifest_modules:
        if manifest.path == repo_module.path:
            yield (manifest, repo_module)
            return
    if repo_module.path in _REPO_PATH_OVERRIDES:
        for manifest in manifest_modules:
            if manifest.path == _REPO_PATH_OVERRIDES[repo_module.path]:
                yield (manifest, repo_module)
                return
    LOG.warning(f"repo module {repo_module.name} had no entry in manifest")


def match_manifest_and_repo(
    manifest_modules: list[SubmoduleSpec], repo_modules: list[SubmoduleSpec]
) -> list[tuple[SubmoduleSpec, SubmoduleSpec]]:
    matched_manifest_modules: list[str] = []
    for repo in repo_modules:
        matches = list(match_manifest_to_repo(manifest_modules, repo))
        matched_manifest_modules.extend(match.name for match, _ in matches)
        yield from iter(matches)
    for manifest_module in manifest_modules:
        if manifest_module.name not in matched_manifest_modules:
            LOG.warning(f"manifest module {manifest_module.name} is not in the repo")


def rationalize(
    manifest_module: SubmoduleSpec,
    repo_module: SubmoduleSpec,
    oecore_path: Path,
    update: bool,
) -> None:
    action_note: str
    if manifest_module.sha != repo_module.sha:
        if update:
            action_note = "WILL CHANGE"
        else:
            action_note = "WOULD CHANGE"
    else:
        if update:
            action_note = "WILL NOT CHANGE"
        else:
            action_note = "WOULD NOT CHANGE"
    print(
        f"{repo_module.path}: {action_note}: current sha {repo_module.sha}, manifest sha {manifest_module.sha}"
    )
    if (manifest_module.sha != repo_module.sha) and update:
        LOG.info(f'Fetching in {repo_module.path}')
        fetch_result = git(
            ['fetch'], cwd=str(oecore_path / repo_module.path)
        )
        LOG.debug(fetch_result)
        LOG.info(f'Checking out manifest sha {manifest_module.sha} in {repo_module.path}')
        result = git(
            ["checkout", manifest_module.sha], cwd=str(oecore_path / repo_module.path)
        )
        LOG.debug(result)


def _do_run(
    working_dir: str | None,
    manifest_ref: str,
    manifest_repo: str,
    manifest_name: str,
    oecore_path: str,
    update: bool,
    quiet: bool,
) -> None:
    with provide_dir(working_dir) as checked_working_dir:
        manifest_path = prep_manifest_repo(
            checked_working_dir, manifest_repo, manifest_ref, quiet
        )
        manifest_modules = parse_manifest_tree(
            manifest_path, Path("tdxref") / f"{manifest_name}.xml"
        )
        oecore_modules = parse_current_repo(Path(oecore_path))
        matched = list(match_manifest_and_repo(manifest_modules, oecore_modules))
        for manifest_module, repo_module in matched:
            rationalize(manifest_module, repo_module, Path(oecore_path), update)


def _run(desc: str) -> int:
    parser = argparse.ArgumentParser(description=desc)
    parser.add_argument(
        "ref",
        metavar="REF",
        action="store",
        help=(
            "A git reflike (suitable for checkout) of the manifest repo to use. Find the appropriate one "
            "from the cgit https://git.toradex.com/cgit/toradex-manifest.git or from the releases "
            "https://developer.toradex.com/linux-bsp/os-development/build-yocto/build-a-reference-image-with-yocto-projectopenembedded/#versions-and-source-code"
        ),
    )
    parser.add_argument(
        "--update",
        action="store_true",
        dest="update",
        help="Actually alter the repo - without passing this, it will only print what it would have done.",
    )
    verbosity_group = parser.add_mutually_exclusive_group()
    verbosity_group.add_argument(
        "-q",
        "--quiet",
        dest="quiet",
        action="store_const",
        const=-1,
        default=0,
        help="Print only errors, and those to stderr",
    )
    verbosity_group.add_argument(
        "-v",
        "--verbose",
        dest="verbose",
        action="count",
        default=0,
        help="Print more and more information, to stdout",
    )

    parser.add_argument(
        "-w",
        "--working-dir",
        dest="working_dir",
        action="store",
        default=None,
        help="Working directory to put the temporary checkout of the manifest repo.",
    )

    parser.add_argument(
        "--manifest-url", dest="manifest_url", action="store", default=_MANIFEST_REPO
    )
    parser.add_argument(
        "--manifest-name", dest="manifest_name", action="store", default="default"
    )
    parser.add_argument(
        "--oecore-path",
        dest="oecore_path",
        action="store",
        default=str(Path(__file__).parent),
    )

    args = parser.parse_args()
    v_levels = [logging.ERROR, logging.WARNING, logging.INFO, logging.DEBUG]
    logging.basicConfig(
        level=v_levels[min(args.quiet + args.verbose + 1, len(v_levels) - 1)],
        stream=sys.stderr if args.quiet else sys.stdout,
        format="%(levelname)s %(message)s",
    )
    try:
        _do_run(
            working_dir=args.working_dir,
            manifest_ref=args.ref,
            manifest_repo=args.manifest_url,
            manifest_name=args.manifest_name,
            oecore_path=args.oecore_path,
            update=args.update,
            quiet=args.quiet,
        )
    except BaseException:
        traceback.print_exc()
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(_run(__doc__))
