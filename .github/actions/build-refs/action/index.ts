import { getOctokit } from '@actions/github'
import * as core from '@actions/core'
import * as semver from 'semver'

export type Repo = 'oe-core' | 'monorepo' | 'ot3-firmware'
export type BuildType = 'develop' | 'release'
export type Variant = 'internal-release' | 'release'
const orderedRepos: Repo[] = ['monorepo', 'oe-core', 'ot3-firmware']

export interface BuildDetails {
  buildType: BuildType
  buildVariant: Variant
}

export type Branch = string
export type Tag = string
export type Ref = Branch | Tag

export type InputRefs = Map<Repo, Ref | null>

export type AttemptableTag = Tag | ':latest:'
export type AttemptableRef = AttemptableTag | Branch

export type AttemptableRefs = Map<Repo, AttemptableRef[]>

export type OutputRefs = Map<Repo, Ref>

function mainRefFor(input: Repo): Branch {
  return {
    monorepo: 'refs/heads/edge',
    'oe-core': 'refs/heads/main',
    'ot3-firmware': 'refs/heads/main',
  }[input]
}

export function restAPICompliantRef(input: Ref): string {
  return input.replace('refs/', '')
}

export interface GitHubApiTag {
  ref: Tag
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

function latestTagPrefixFor(repo: Repo, variant: Variant): string[] {
  if (variant === 'release') {
    return ['refs/tags/v']
  }
  if (variant === 'internal-release') {
    if (repo === 'monorepo') return ['refs/tags/internal@', 'refs/tags/ot3@v']
    if (repo === 'oe-core') return ['refs/tags/internal@']
    if (repo === 'ot3-firmware') return ['refs/tags/internal@']
    throw new Error(`Unknown repo ${repo}`)
  }
  throw new Error(`Unknown variant ${variant}`)
}

export function latestTag(tagRefs: GitHubApiTag[]): Tag | null {
  if (tagRefs.length === 0) return null

  // Extract and parse version numbers from tag refs
  const tagVersions = tagRefs
    .map(tag => {
      const tagName = tag.ref.replace('refs/tags/', '')

      // Handle v* tags (e.g., "v1.19.4" or "v66")
      if (tagName.startsWith('v')) {
        const version = tagName.substring(1)
        // Accept both semantic versions and simple numeric versions like "v66"
        const isValidSemver = semver.valid(version)
        const isValidNumeric = /^\d+$/.test(version)
        return {
          tag: tag.ref,
          version,
          isValid: isValidSemver || isValidNumeric,
        }
      }

      // Handle internal@* tags (e.g., "internal@1.2.0-alpha.0" or "internal@v23")
      if (tagName.startsWith('internal@')) {
        let version = tagName.substring(9) // Remove "internal@"
        // Handle internal@v* format by removing the 'v' prefix
        if (version.startsWith('v')) {
          version = version.substring(1)
        }
        // Accept both semantic versions and simple numeric versions
        const isValidSemver = semver.valid(version)
        const isValidNumeric = /^\d+$/.test(version)
        return {
          tag: tag.ref,
          version,
          isValid: isValidSemver || isValidNumeric,
        }
      }

      // Handle ot3@* tags (e.g., "ot3@1.2.0-alpha.0")
      if (tagName.startsWith('ot3@')) {
        const version = tagName.substring(4) // Remove "ot3@"
        return { tag: tag.ref, version, isValid: semver.valid(version) }
      }

      // Unknown tag format
      return { tag: tag.ref, version: null, isValid: false }
    })
    .filter(tv => tv.isValid) // Only keep valid versions (semantic or numeric)

  if (tagVersions.length === 0) return null

  // Sort by version and return the latest
  tagVersions.sort((a, b) => {
    const aIsSemver = semver.valid(a.version!)
    const bIsSemver = semver.valid(b.version!)

    if (aIsSemver && bIsSemver) {
      return semver.compare(a.version!, b.version!)
    } else if (aIsSemver && !bIsSemver) {
      // Semantic versions are considered newer than numeric versions
      return 1
    } else if (!aIsSemver && bIsSemver) {
      // Numeric versions are considered older than semantic versions
      return -1
    } else {
      // Both are numeric versions, compare numerically
      return parseInt(a.version!) - parseInt(b.version!)
    }
  })
  return tagVersions[tagVersions.length - 1].tag
}

function restDetailsFor(input: Repo): { owner: string; repo: string } {
  return {
    monorepo: { owner: 'Opentrons', repo: 'opentrons' },
    'oe-core': { owner: 'Opentrons', repo: 'oe-core' },
    'ot3-firmware': { owner: 'Opentrons', repo: 'ot3-firmware' },
  }[input]
}

function refIsMain(input: Ref, repo: Repo): boolean {
  return mainRefFor(repo) === input
}

export function authoritativeRef(inputs: InputRefs): [Ref, boolean] {
  return (
    orderedRepos
      .map((repoName): [Ref, boolean] | null => {
        const inputRefForRepo = inputs.get(repoName)
        return inputRefForRepo
          ? [inputRefForRepo, refIsMain(inputRefForRepo, repoName)]
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
  requesterIsMain: boolean,
  requestedMain: Branch
): Ref[] {
  // if this is a main-branch build, use our main branch
  if (requesterIsMain) {
    return [requestedMain]
  }
  // otherwise, use a matching branchname and then our main branch
  return [requesterBranch, requestedMain]
}

function tagsToAttempt(
  requesterTag: Tag,
  requestedMain: Branch
): AttemptableTag[] {
  return [requesterTag, ':latest:', requestedMain]
}

export function refsToAttempt(
  requesterRef: Ref,
  requesterIsMain: boolean,
  requestedMain: Branch
): Ref[] {
  ///Based on the refs from whatever was specified, return an ordered list of refs to
  // try.
  return visitRefsByType(
    requesterRef,
    requesterBranch =>
      branchesToAttempt(requesterBranch, requesterIsMain, requestedMain),
    requesterTag => tagsToAttempt(requesterTag, requestedMain)
  )
}

async function resolveRefs(
  toAttempt: AttemptableRefs,
  variant: Variant
): Promise<OutputRefs> {
  const token = core.getInput('token')
  let resolved = new Map()
  for (const [repo, refList] of toAttempt) {
    const octokit = getOctokit(token)

    const fetchTags = async (repoName: Repo): Promise<Ref | null> => {
      core.info(`finding latest tag for ${repoName}`)
      return Promise.all(
        latestTagPrefixFor(repoName, variant).map(prefix =>
          octokit.rest.git
            .listMatchingRefs({
              ...restDetailsFor(repoName),
              ref: restAPICompliantRef(prefix),
            })
            .then((response: any) => {
              if (response.status != 200) {
                throw new Error(
                  `Bad response from github api for ${repoName} get tags: ${response.status}`
                )
              }
              const latest = latestTag(response.data)
              core.debug(
                `latest tag for ${repoName} variant ${variant} is ${latest}`
              )
              return latest
            })
        )
      ).then(results =>
        results.reduce((prev: any, current: any) => prev ?? current, null)
      )
    }

    // this is a big function to be inline and untestable, but tookit doesn't export
    // the type for the octokit object above so what are you gonna do
    const refResolves = async (
      repoName: Repo,
      ref: AttemptableRef
    ): Promise<Ref | null> => {
      core.info(`looking for ${ref} on ${repoName}`)
      const correctRef = ref === ':latest:' ? await fetchTags(repoName) : ref
      if (correctRef === null) {
        core.info(`couldn't find ref ${correctRef} for ${ref} on ${repoName}`)
        return null
      }

      return octokit.rest.git
        .listMatchingRefs({
          ...restDetailsFor(repoName),
          ref: restAPICompliantRef(correctRef),
        })
        .then((value: any) => {
          if (value.status != 200 || !value.data) {
            throw new Error(
              `Bad response from github api for ${repoName} get matching refs: ${value.status}`
            )
          }
          const availableRefs = value.data.map((refObj: any) => refObj.ref)
          core.info(`refs on ${repoName} matching ${ref}: ${availableRefs}`)
          return availableRefs.includes(correctRef) ? correctRef : null
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

async function run() {
  const inputs = getInputs()
  inputs.forEach((ref, repo) => {
    core.debug(`found input for ${repo}: ${ref}`)
  })
  const [authoritative, isMain] = authoritativeRef(inputs)
  core.debug(`authoritative ref is ${authoritative} (main: ${isMain})`)
  const buildType = resolveBuildType(authoritative)
  const buildVariant = resolveBuildVariant(authoritative)
  core.info(`Resolved build type to ${buildType}`)
  core.info(`Resolved build variant to ${buildVariant}`)

  const attemptable = Array.from(inputs.entries()).reduce(
    (prev: AttemptableRefs, [repoName, inputRef]): AttemptableRefs => {
      return prev.set(
        repoName,
        inputRef
          ? [inputRef]
          : refsToAttempt(authoritative, isMain, mainRefFor(repoName))
      )
    },
    new Map()
  )
  attemptable.forEach((refs, repo) => {
    core.debug(`found attemptable refs for ${repo}: ${refs.join(', ')}`)
  })
  core.setOutput('build-type', buildType)
  core.setOutput('variant', buildVariant)

  const resolved = await resolveRefs(attemptable, buildVariant)
  resolved.forEach((ref, repo) => {
    if (!ref) {
      throw new Error(
        `Could not resolve ${repo} input reference ${inputs.get(repo)}`
      )
    }
    core.info(`Resolved ${repo} to ${ref}`)
    core.setOutput(repo, ref)
  })
}

async function _run() {
  try {
    await run()
  } catch (error: any) {
    core.setFailed(error.toString())
  }
}

_run()
