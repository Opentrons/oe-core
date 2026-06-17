import { getOctokit } from '@actions/github'
import * as core from '@actions/core'
import * as fs from 'fs'

export type Repo = 'oe-core' | 'monorepo' | 'ot3-firmware'
export type BuildType = 'develop' | 'release'
export type Variant = 'internal-release' | 'release'
const orderedRepos: Repo[] = ['monorepo', 'oe-core', 'ot3-firmware']
const FIRMWARE_REPO: Repo = 'ot3-firmware'

export interface BuildDetails {
  buildType: BuildType
  buildVariant: Variant
}

export type Branch = string
export type Tag = string
export type Ref = Branch | Tag

export type InputRefs = Map<Repo, Ref | null>

export type AttemptableRef = Tag | Branch

export type AttemptableRefs = Map<Repo, AttemptableRef[]>

export type OutputRefs = Map<Repo, Ref>

function defaultBranchRefFor(input: Repo): Branch {
  return {
    monorepo: 'refs/heads/edge',
    'oe-core': 'refs/heads/main',
    'ot3-firmware': 'refs/heads/main',
  }[input]
}

export function restAPICompliantRef(input: Ref): string {
  return input.replace('refs/', '')
}

export function tagNameFromRef(ref: Tag): string {
  return ref.slice('refs/tags/'.length)
}

/** Integer-only firmware version tags such as v70 (not stack semver v9.1.0). */
export function isFirmwareVersionTagRef(ref: Ref): boolean {
  if (!ref.startsWith('refs/tags/v')) {
    return false
  }
  return /^v\d+$/.test(tagNameFromRef(ref as Tag))
}

/**
 * Map a stack coordinated release tag to the ot3-firmware coordination tag (ex* prefix).
 * External stack semver v9.1.0-alpha.7 becomes ex9.1.0-alpha.7. Internal ot3@* is unchanged.
 * Integer firmware version tags (v70) are not mapped here; release process places exactly
 * one vN on the coordination commit (reused across releases when firmware does not bump).
 */
export function stackCoordinatedTagToFirmwareTag(stackTagRef: Tag): Tag | null {
  if (!stackTagRef.startsWith('refs/tags/')) {
    return null
  }
  const name = tagNameFromRef(stackTagRef)
  if (name.startsWith('ex')) {
    return null
  }
  if (name.startsWith('v') && !/^v\d+$/.test(name)) {
    return `refs/tags/ex${name.slice(1)}` as Tag
  }
  return null
}

/** Normalize an ot3-firmware input ref (map external stack v* tags to ex* on firmware). */
export function normalizeFirmwareInputRef(ref: Ref): Ref {
  if (!ref.startsWith('refs/tags/')) {
    return ref
  }
  return stackCoordinatedTagToFirmwareTag(ref as Tag) ?? ref
}

export function expectedFirmwareTagForAuthoritative(authoritative: Ref): Ref {
  if (!authoritative.startsWith('refs/tags/')) {
    return authoritative
  }
  return stackCoordinatedTagToFirmwareTag(authoritative as Tag) ?? authoritative
}

/** Human-readable note for one repo row in the workflow step summary. */
export function summaryNoteForRepo(
  repo: Repo,
  authoritative: Ref,
  resolved: Ref,
  inputRef: Ref | null
): string {
  if (authoritative.startsWith('refs/heads/')) {
    return repo === FIRMWARE_REPO
      ? 'Branch build; matching branch or default (no ex* tag mapping)'
      : 'Branch build; matching branch or default'
  }

  if (repo === FIRMWARE_REPO) {
    const mappedFromAuthoritative = stackCoordinatedTagToFirmwareTag(
      authoritative as Tag
    )
    if (mappedFromAuthoritative && mappedFromAuthoritative === resolved) {
      return `Mapped stack tag ${tagNameFromRef(authoritative as Tag)} → ${tagNameFromRef(resolved as Tag)}`
    }
    if (
      inputRef?.startsWith('refs/tags/') &&
      normalizeFirmwareInputRef(inputRef) === resolved &&
      normalizeFirmwareInputRef(inputRef) !== inputRef
    ) {
      return `Mapped explicit input ${tagNameFromRef(inputRef as Tag)} → ${tagNameFromRef(resolved as Tag)}`
    }
    return 'Coordination tag on firmware (internal ot3@*, or ex* specified explicitly)'
  }

  return 'Stack coordination tag'
}

async function writeBuildRefsSummary(
  authoritative: Ref,
  isDefaultBranch: boolean,
  inputs: InputRefs,
  resolved: OutputRefs,
  buildType: BuildType,
  buildVariant: Variant
): Promise<void> {
  const authKind = authoritative.startsWith('refs/tags/')
    ? 'tag build'
    : `branch build${isDefaultBranch ? ' (default branch)' : ''}`

  const rows: Array<Array<{ data: string; header?: boolean }>> = [
    [
      { data: 'Repo', header: true },
      { data: 'Input', header: true },
      { data: 'Resolved ref', header: true },
      { data: 'Notes', header: true },
    ],
  ]

  for (const repo of orderedRepos) {
    const input = inputs.get(repo)
    rows.push([
      { data: repo },
      { data: input ?? '(from authoritative ref)' },
      { data: resolved.get(repo)! },
      {
        data: summaryNoteForRepo(
          repo,
          authoritative,
          resolved.get(repo)!,
          input ?? null
        ),
      },
    ])
  }

  const summary = core.summary
    .addHeading('Build refs', 3)
    .addRaw(`**Authoritative ref:** \`${authoritative}\` (${authKind})`, true)
    .addEOL()
    .addRaw(
      `**Build type:** \`${buildType}\` · **Variant:** \`${buildVariant}\``,
      true
    )
    .addEOL()
    .addTable(rows)
    .addEOL()

  if (
    authoritative.startsWith('refs/tags/v') &&
    !isFirmwareVersionTagRef(authoritative)
  ) {
    summary.addRaw(
      '> **External tag mapping:** stack semver tags (`vX.Y.Z*`) on opentrons and oe-core map to `exX.Y.Z*` on ot3-firmware only. That keeps stack semver off ot3-firmware.',
      true
    )
  } else if (authoritative.startsWith('refs/tags/ot3@')) {
    summary.addRaw(
      '> **Internal tag build:** the same `ot3@*` coordination tag is used on all three repos. ot3-firmware still needs an integer `vN` tag on that commit for cmake.',
      true
    )
  }

  await summary.write()
}

export function resolveBuildVariant(ref: Ref): Variant {
  if (ref.startsWith('refs/heads')) {
    if (ref.includes('internal-release')) {
      return 'internal-release'
    } else {
      return 'release'
    }
  } else if (ref.startsWith('refs/tags')) {
    if (ref.includes('ot3@') || ref.includes('internal@')) {
      return 'internal-release'
    } else if (ref.startsWith('refs/tags/v')) {
      return 'release'
    }
  }
  return 'internal-release'
}

function restDetailsFor(input: Repo): { owner: string; repo: string } {
  return {
    monorepo: { owner: 'Opentrons', repo: 'opentrons' },
    'oe-core': { owner: 'Opentrons', repo: 'oe-core' },
    'ot3-firmware': { owner: 'Opentrons', repo: 'ot3-firmware' },
  }[input]
}

function refIsDefaultBranch(input: Ref, repo: Repo): boolean {
  return defaultBranchRefFor(repo) === input
}

export function authoritativeRef(inputs: InputRefs): [Ref, boolean] {
  return (
    orderedRepos
      .map((repoName): [Ref, boolean] | null => {
        const inputRefForRepo = inputs.get(repoName)
        return inputRefForRepo
          ? [inputRefForRepo, refIsDefaultBranch(inputRefForRepo, repoName)]
          : null
      })
      .find(el => el !== null) ?? ['refs/heads/edge', true]
  )
}

function getInputs(): InputRefs {
  return orderedRepos.reduce((prev: InputRefs, inputName: Repo): InputRefs => {
    const input = core.getInput(inputName)
    return prev.set(inputName, input == '-' ? null : input)
  }, new Map())
}

function visitRefsByType<T>(
  ref: Ref,
  ifBranch: (branch: Branch) => T,
  ifTag: (tag: Tag) => T
): T {
  if (ref.startsWith('refs/heads')) return ifBranch(ref as Branch)
  if (ref.startsWith('refs/tags')) return ifTag(ref as Tag)
  throw new Error(
    `Ref ${ref} can't be matched to branch or tag, is it a shortref?`
  )
}

function branchesToAttempt(
  requesterBranch: Branch,
  requesterIsDefaultBranch: boolean,
  requestedDefaultBranch: Branch
): Ref[] {
  // if this is a default-branch build, use our default branch
  if (requesterIsDefaultBranch) {
    return [requestedDefaultBranch]
  }
  // otherwise, use a matching branchname and then our default branch
  return [requesterBranch, requestedDefaultBranch]
}

/** Return whether a ref is a coordinated Flex release tag (ot3@* or v*). */
export function isCoordinatedReleaseTag(ref: Ref): boolean {
  if (!ref.startsWith('refs/tags/')) {
    return false
  }
  const tagName = ref.slice('refs/tags/'.length)
  return tagName.startsWith('ot3@') || tagName.startsWith('v')
}

function tagsToAttempt(requesterTag: Tag, repo?: Repo): Ref[] {
  if (repo === FIRMWARE_REPO) {
    const firmwareTag = stackCoordinatedTagToFirmwareTag(requesterTag)
    if (firmwareTag) {
      return [firmwareTag]
    }
  }
  // Tag builds require the matching tag on every repo; no default-branch fallback.
  return [requesterTag]
}

export function refsToAttempt(
  requesterRef: Ref,
  requesterIsDefaultBranch: boolean,
  requestedDefaultBranch: Branch,
  repo?: Repo
): Ref[] {
  ///Based on the refs from whatever was specified, return an ordered list of refs to
  // try.
  return visitRefsByType(
    requesterRef,
    requesterBranch =>
      branchesToAttempt(
        requesterBranch,
        requesterIsDefaultBranch,
        requestedDefaultBranch
      ),
    requesterTag => tagsToAttempt(requesterTag, repo)
  )
}

async function resolveRefs(toAttempt: AttemptableRefs): Promise<OutputRefs> {
  const token = core.getInput('token')
  let resolved = new Map()
  for (const [repo, refList] of toAttempt) {
    const octokit = getOctokit(token)

    // this is a big function to be inline and untestable, but tookit doesn't export
    // the type for the octokit object above so what are you gonna do
    const refResolves = async (
      repoName: Repo,
      ref: AttemptableRef
    ): Promise<Ref | null> => {
      core.info(`looking for ${ref} on ${repoName}`)

      return octokit.rest.git
        .listMatchingRefs({
          ...restDetailsFor(repoName),
          ref: restAPICompliantRef(ref),
        })
        .then((value: any) => {
          if (value.status != 200 || !value.data) {
            throw new Error(
              `Bad response from github api for ${repoName} get matching refs: ${value.status}`
            )
          }
          const availableRefs = value.data.map((refObj: any) => refObj.ref)
          core.info(`refs on ${repoName} matching ${ref}: ${availableRefs}`)
          return availableRefs.includes(ref) ? ref : null
        })
    }

    resolved.set(
      repo,
      await Promise.all(refList.map(ref => refResolves(repo, ref))).then(
        presentRefs => presentRefs.find(maybeRef => maybeRef !== null) || null
      )
    )
  }
  return resolved
}

export function resolveBuildType(ref: Ref): BuildType {
  return ref.includes('refs/tags') ? 'release' : 'develop'
}

function setOutput(name: string, value: string): void {
  const outputFile = process.env['GITHUB_OUTPUT']
  if (!outputFile) {
    throw new Error('GITHUB_OUTPUT environment variable is not set')
  }

  // Append to the output file with proper formatting
  const output = `${name}=${value}\n`
  fs.appendFileSync(outputFile, output)
}

async function run() {
  const token = core.getInput('token')
  const inputs = getInputs()
  inputs.forEach((ref, repo) => {
    core.debug(`found input for ${repo}: ${ref}`)
  })
  const [authoritative, isDefaultBranch] = authoritativeRef(inputs)
  core.debug(
    `authoritative ref is ${authoritative} (default branch: ${isDefaultBranch})`
  )
  const buildType = resolveBuildType(authoritative)
  const buildVariant = resolveBuildVariant(authoritative)
  core.info(`Resolved build type to ${buildType}`)
  core.info(`Resolved build variant to ${buildVariant}`)

  const attemptable = Array.from(inputs.entries()).reduce(
    (prev: AttemptableRefs, [repoName, inputRef]): AttemptableRefs => {
      const normalizedInput =
        repoName === FIRMWARE_REPO && inputRef
          ? normalizeFirmwareInputRef(inputRef)
          : inputRef
      if (
        repoName === FIRMWARE_REPO &&
        inputRef &&
        normalizedInput !== inputRef
      ) {
        core.info(`Mapped ot3-firmware input ${inputRef} to ${normalizedInput}`)
      }
      return prev.set(
        repoName,
        normalizedInput
          ? [normalizedInput]
          : refsToAttempt(
              authoritative,
              isDefaultBranch,
              defaultBranchRefFor(repoName),
              repoName
            )
      )
    },
    new Map()
  )
  attemptable.forEach((refs, repo) => {
    core.debug(`found attemptable refs for ${repo}: ${refs.join(', ')}`)
  })
  setOutput('build-type', buildType)
  setOutput('variant', buildVariant)
  setOutput('authoritative-ref', authoritative)

  const resolved = await resolveRefs(attemptable)
  for (const [repo, ref] of resolved.entries()) {
    if (!ref) {
      const inputRef = inputs.get(repo)
      const tagHint =
        authoritative.startsWith('refs/tags/') && inputRef === null
          ? repo === FIRMWARE_REPO
            ? ` Tag builds require ${expectedFirmwareTagForAuthoritative(authoritative)} on ot3-firmware.`
            : ` Tag builds require the same tag (${authoritative}) on all repos.`
          : ''
      throw new Error(
        `Could not resolve ${repo} input reference ${inputRef}.${tagHint}`
      )
    }
  }

  resolved.forEach((ref, repo) => {
    core.info(`Resolved ${repo} to ${ref}`)
    setOutput(repo, ref!)
  })

  await writeBuildRefsSummary(
    authoritative,
    isDefaultBranch,
    inputs,
    resolved,
    buildType,
    buildVariant
  )
}

async function _run() {
  try {
    await run()
  } catch (error: any) {
    core.setFailed(error.toString())
  }
}

_run()
