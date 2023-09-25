import { getOctokit } from '@actions/github'
import * as core from '@actions/core'

export type Repo = 'oe-core' | 'monorepo' | 'ot3-firmware'
export type BuildType = 'develop' | 'release'
export type Variant = 'internal-release' | 'release'
export type BuildArg = string | null
export type BuildArgs = Map<string, BuildArg>
let buildArgs = new BuildArgs([
   ["oe-core", "-"],
   ["monorepo", "-"],
   ["ot3-firmware", "-"],
   ["build-type", "develop"],
   ["variant", "internal-release"],
])

export function getBuildArgs(): BuildArgs {
   buildArgs.forEach(function(val: string, key: BuildArg) {
      core.info(`input: {key} = {val}`)
   })
   return buildArgs
}

async function run() {
   getBuildArgs()
   core.setOutput('queue-build', false)
}

async function _run() {
  try {
    await run()
  } catch (error: any) {
    core.setFailed(error.toString())
  }
}

_run()
