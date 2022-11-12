import * as github from "@actions/github";
import * as core from "@actions/core";

type Repo = "oe-core" | "monorepo";

const orderedRepos: Repo[] = ["monorepo", "oe-core"];

type LongBranch = string;
type LongTag = string;
type ShortBranch = string;
type ShortTag = string;

type Branch = LongBranch | ShortBranch;
type Tag = LongTag | ShortTag;

type ShortRef = ShortBranch | ShortTag;
type LongRef = LongBranch | LongTag;
type Ref = ShortRef | LongRef;

type InputRefs = Map<Repo, LongRef | null>;

type AttemptableTag = LongTag | ":latest:";
type AttemptableRef = AttemptableTag | LongBranch;

type AttemptableRefs = Map<Repo, AttemptableRef[]>;

type OutputRefs = Map<Repo, ShortRef>;

function mainRefFor(input: Repo): LongBranch {
  return { monorepo: "refs/heads/edge", "oe-core": "refs/heads/edge" }[input];
}

function restDetailsFor(input: Repo): { owner: string; repo: string } {
  return {
    monorepo: { owner: "Opentrons", repo: "opentrons" },
    "oe-core": { owner: "Opentrons", repo: "oe-core" },
  }[input];
}

function refIsMain(input: Ref, repo: Repo): boolean {
  return mainRefFor(repo) === shortenRef(input);
}

export function authoritativeRef(inputs: InputRefs): [Ref, boolean] {
  return (
    orderedRepos
      .map((repoName): [Ref, boolean] | null =>
        inputs[repoName]
          ? [inputs[repoName], refIsMain(inputs[repoName], repoName)]
          : null
      )
      .find((el) => el !== null) ?? ["refs/heads/edge", true]
  );
}

function getInputs(): InputRefs {
  return orderedRepos.reduce((prev: InputRefs, inputName: Repo): InputRefs => {
    const input = core.getInput(inputName);
    return { [inputName]: input == "-" ? null : input, ...prev };
  }, new Map());
}

function shortenRef(ref: Ref): ShortRef {
  if (ref.startsWith("refs/heads")) return shortenBranch(ref as Branch);
  if (ref.startsWith("refs/tags")) return shortenTag(ref as Tag);
  return ref;
}

function shortenBranch(branch: Branch): ShortBranch {
  return branch.match(/(?<optionalLong>refs\/heads\/)?(?<branchName>.*)/).groups
    .branchName;
}

function shortenTag(tag: Tag): ShortTag {
  return tag.match(/(?<optionalLong>refs\/tags\/)?(?<tagName>.*)/).groups
    .tagName;
}

function branchesToAttempt(
  requesterBranch: ShortBranch,
  requesterIsMain: boolean,
  requestedMain: ShortBranch
): ShortRef[] {
  // if this is a main-branch build, use our main branch
  if (requesterIsMain) {
    return [requestedMain];
  }
  // otherwise, use a matching branchname and then our main branch
  return [requesterBranch, requestedMain];
}

function tagsToAttempt(
  requesterTag: ShortTag,
  requestedMain: ShortBranch
): AttemptableTag[] {
  return [requesterTag, ":latest:", requestedMain];
}

function refsToAttempt(
  requesterRef: Ref,
  requesterIsMain: boolean,
  requestedMain: ShortBranch
): ShortRef[] {
  ///Based on the refs from whatever was specified, return an ordered list of refs to
  // try.

  if (requesterRef.startsWith("refs/heads")) {
    return branchesToAttempt(
      requesterRef as LongBranch,
      requesterIsMain,
      requestedMain
    );
  }
  if (requesterRef.startsWith("refs/tags")) {
    return tagsToAttempt(requesterRef as LongTag, requestedMain);
  }
  throw new Error(
    "Could not parse input ref ${requesterRef}, defaulting to ${requestedMain}. Please use long refs"
  );
}

async function refResolves(
  repoName: Repo,
  ref: AttemptableRef,
  octokit
): Promise<LongRef | null> {
  core.info("looking for ${ref} on ${repoName}");
  return octokit.rest.git.listMatchingRefs
    .get({
      ...restDetailsFor(repoName),
      ref: ref,
    })
    .then((value) => {
      const availableRefs = [value].flatMap((refObj) => refObj.ref);
      core.info("refs on ${repoName} matching ${ref}: ${availableRefs}");
      return availableRefs.includes(ref) ? ref : null;
    });
}

async function resolveRefs(toAttempt: AttemptableRefs): Promise<OutputRefs> {
  const token = core.getInput("token");
  let resolved = new Map();
  for (const [repo, refsToAttempt] of toAttempt) {
    const octokit = github.getOctokit(token);
    resolved.set(
      repo,
      await Promise.all(
        refsToAttempt.map((ref) => refResolves(repo, ref, octokit))
      ).then((presentRefs) => presentRefs.find((maybeRef) => maybeRef !== null))
    );
  }
  return resolved;
}

async function run() {
  const inputs = getInputs();
  const [authoritative, isMain] = authoritativeRef(inputs);
  const attemptable = Array.from(inputs.entries()).reduce(
    (prev: AttemptableRefs, [repoName, inputRef]): AttemptableRefs => {
      return prev.set(
        repoName,
        inputRef
          ? [shortenRef(inputRef)]
          : refsToAttempt(authoritative, isMain, mainRefFor(repoName))
      );
    },
    new Map()
  );
  const resolved = await resolveRefs(attemptable);
  resolved.forEach((ref, repo) => {
    core.info("Resolved ${repo} to ${ref}");
    core.setOutput(repo, shortenRef(ref));
  });
}

async function _run() {
  try {
    await run();
  } catch (error) {
    core.setFailed(error.message);
  }
}

_run();
