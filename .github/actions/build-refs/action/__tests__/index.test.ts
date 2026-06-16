import * as action from '../index'
import type { InputRefs, Ref, Branch, BuildType, Variant } from '../index'

const AUTHORITATIVE_REF_TEST_SPECS: Array<
  [string, InputRefs, [string, boolean]]
> = [
  [
    'prefers monorepo if oe-core not specified',
    new Map([
      ['oe-core', null],
      ['monorepo', 'refs/heads/edge'],
    ]),
    ['refs/heads/edge', true],
  ],
  [
    'uses oe-core if monorepo not specified',
    new Map([
      ['oe-core', 'refs/heads/main'],
      ['monorepo', null],
    ]),
    ['refs/heads/main', true],
  ],
  [
    'prefers monorepo if both specified',
    new Map([
      ['oe-core', 'refs/heads/main'],
      ['monorepo', 'refs/heads/edge'],
    ]),
    ['refs/heads/edge', true],
  ],
  [
    'identifies if monorepo preferred but on non-default branch',
    new Map([
      ['oe-core', 'refs/heads/main'],
      ['monorepo', 'refs/heads/some-test-branch'],
    ]),
    ['refs/heads/some-test-branch', false],
  ],
  [
    'identifies if oe-core preferred but on non-default branch',
    new Map([
      ['oe-core', 'refs/heads/some-test-branch'],
      ['monorepo', null],
    ]),
    ['refs/heads/some-test-branch', false],
  ],
  [
    'falls back to default branches if nothing is specified',
    new Map([
      ['oe-core', null],
      ['monorepo', null],
    ]),
    ['refs/heads/edge', true],
  ],
]

AUTHORITATIVE_REF_TEST_SPECS.forEach(
  ([testNameFragment, inputRefs, result]) => {
    test(`authoritativeRef ${testNameFragment}`, () => {
      expect(action.authoritativeRef(inputRefs)).toStrictEqual(result)
    })
  }
)

const REFS_TO_ATTEMPT_TEST_SPECS: Array<
  [string, [Ref, boolean, Branch], Ref[]]
> = [
  [
    'with non-default branches should prioritize the request and then use default branch',
    ['refs/heads/someBranch', false, 'refs/heads/someDefaultBranch'],
    ['refs/heads/someBranch', 'refs/heads/someDefaultBranch'],
  ],
  [
    'with default branches should just use default branch',
    ['refs/heads/someOtherDefaultBranch', true, 'refs/heads/someDefaultBranch'],
    ['refs/heads/someDefaultBranch'],
  ],
  [
    'with tags should require the matching tag only',
    ['refs/tags/someTag', false, 'refs/heads/someDefaultBranch'],
    ['refs/tags/someTag'],
  ],
  [
    'with coordinated ot3 tags should require the matching tag only',
    ['refs/tags/ot3@8.5.0-alpha.0', false, 'refs/heads/someDefaultBranch'],
    ['refs/tags/ot3@8.5.0-alpha.0'],
  ],
  [
    'with external firmware tags should require the matching tag only',
    ['refs/tags/v10.0.0-beta.1', false, 'refs/heads/someDefaultBranch'],
    ['refs/tags/v10.0.0-beta.1'],
  ],
]

REFS_TO_ATTEMPT_TEST_SPECS.forEach(
  ([
    testNameFragment,
    [
      testRequesterRef,
      testRequesterIsDefaultBranch,
      testRequestedDefaultBranch,
    ],
    testResults,
  ]) => {
    test(`refsToAttempt ${testNameFragment}`, () => {
      expect(
        action.refsToAttempt(
          testRequesterRef,
          testRequesterIsDefaultBranch,
          testRequestedDefaultBranch
        )
      ).toStrictEqual(testResults)
    })
  }
)

const REFS_TO_ATTEMPT_FAILURE_TEST_SPECS: Array<
  [string, [Ref, boolean, Branch]]
> = [
  [
    'when called with a short refname throws',
    ['shortBranchName', true, 'refs/heads/someDefaultBranch'],
  ],
]

REFS_TO_ATTEMPT_FAILURE_TEST_SPECS.forEach(
  ([
    testNameFragment,
    [
      testRequesterRef,
      testRequesterIsDefaultBranch,
      testRequestedDefaultBranch,
    ],
  ]) => {
    test(`refsToAttempt ${testNameFragment}`, () => {
      expect(() => {
        action.refsToAttempt(
          testRequesterRef,
          testRequesterIsDefaultBranch,
          testRequestedDefaultBranch
        )
      }).toThrow()
    })
  }
)

const BUILD_TYPE_TEST_SPECS: Array<[string, [Ref], BuildType]> = [
  [
    'when monorepo ref is edge but is not a tag is develop',
    ['refs/heads/edge'],
    'develop',
  ],
  [
    'when monorepo ref is some branch but is not a tag is develop',
    ['refs/heads/something'],
    'develop',
  ],
  [
    'when monorepo ref is an internal-release tag is release',
    ['refs/tags/ot3@0.0.0-dev'],
    'release',
  ],
  [
    'when monorepo ref is a release tag is release',
    ['refs/tags/v12.12.9-alpha.12'],
    'release',
  ],
]

BUILD_TYPE_TEST_SPECS.forEach(
  ([testNameFragment, [testMonorepoRef], testExpectedResult]) => {
    test(`buildType ${testNameFragment}`, () => {
      expect(action.resolveBuildType(testMonorepoRef)).toStrictEqual(
        testExpectedResult
      )
    })
  }
)

const VARIANT_TEST_SPECS: Array<[string, Ref, Variant]> = [
  [
    'when monorepo ref is a general branch name',
    'refs/heads/some-random-branch',
    'release',
  ],
  ['when monorepo ref is edge', 'refs/heads/edge', 'release'],
  ['when monorepo ref is release branch', 'refs/heads/main', 'release'],
  [
    'when monorepo ref is a release candidate branch',
    'refs/heads/release_7.1.0',
    'release',
  ],
  [
    'when monorepo ref is a release candidate branch with old style name',
    'refs/heads/chore_release-8.1.0',
    'release',
  ],
  [
    'when monorepo ref is an internal-release branch',
    'refs/heads/internal-release',
    'internal-release',
  ],
  [
    'when monorepo ref is an internal-release candidate branch',
    'refs/heads/internal-release_0.165.0',
    'internal-release',
  ],
  ['when monorepo ref is a release tag', 'refs/tags/v123.213.8', 'release'],
  [
    'when monorepo ref is an internal-release tag',
    'refs/tags/ot3@0.1231.8-alpha.2',
    'internal-release',
  ],
]

VARIANT_TEST_SPECS.forEach(
  ([testNameFragment, testMonorepoRef, testExpectedResult]) => {
    test(`variant ${testNameFragment}`, () => {
      expect(action.resolveBuildVariant(testMonorepoRef)).toStrictEqual(
        testExpectedResult
      )
    })
  }
)

const LATEST_TAG_TEST_SPECS: Array<
  [string, action.GitHubApiTag[], string | null]
> = [
  [
    'handles internal@v* numeric tags correctly',
    [
      { ref: 'refs/tags/internal@v23' },
      { ref: 'refs/tags/internal@v22' },
      { ref: 'refs/tags/internal@v21' },
    ],
    'refs/tags/internal@v23',
  ],
  [
    'handles internal@* semantic version tags correctly',
    [
      { ref: 'refs/tags/internal@1.2.0-alpha.0' },
      { ref: 'refs/tags/internal@1.1.0-alpha.0' },
      { ref: 'refs/tags/internal@1.0.0-alpha.0' },
    ],
    'refs/tags/internal@1.2.0-alpha.0',
  ],
  [
    'handles mixed internal@* tag formats and picks the latest',
    [
      { ref: 'refs/tags/internal@v23' },
      { ref: 'refs/tags/internal@1.2.0-alpha.0' },
      { ref: 'refs/tags/internal@v22' },
    ],
    'refs/tags/internal@1.2.0-alpha.0', // semantic versions are considered newer
  ],
  [
    'handles v* tags correctly',
    [
      { ref: 'refs/tags/v1.19.4' },
      { ref: 'refs/tags/v1.19.3' },
      { ref: 'refs/tags/v66' },
    ],
    'refs/tags/v1.19.4',
  ],
  [
    'handles ot3@* tags correctly',
    [
      { ref: 'refs/tags/ot3@2.8.0-alpha.0' },
      { ref: 'refs/tags/ot3@2.7.0-alpha.0' },
    ],
    'refs/tags/ot3@2.8.0-alpha.0',
  ],
  ['returns null for empty tag list', [], null],
  [
    'filters out invalid tags and returns the latest valid one',
    [
      { ref: 'refs/tags/invalid-tag' },
      { ref: 'refs/tags/internal@v23' },
      { ref: 'refs/tags/another-invalid' },
    ],
    'refs/tags/internal@v23',
  ],
]

LATEST_TAG_TEST_SPECS.forEach(
  ([testNameFragment, testTagRefs, testExpectedResult]) => {
    test(`latestTag ${testNameFragment}`, () => {
      expect(action.latestTag(testTagRefs)).toStrictEqual(testExpectedResult)
    })
  }
)

const COORDINATED_RELEASE_TAG_SPECS: Array<[string, Ref, boolean]> = [
  ['ot3 internal alpha', 'refs/tags/ot3@8.5.0-alpha.0', true],
  ['ot3 internal beta', 'refs/tags/ot3@8.5.0-beta.1', true],
  ['external beta', 'refs/tags/v10.0.0-beta.1', true],
  ['external alpha', 'refs/tags/v10.0.0-alpha.0', true],
  ['edge branch', 'refs/heads/edge', false],
  ['legacy internal@ tag alone', 'refs/tags/internal@8.5.0', false],
]

COORDINATED_RELEASE_TAG_SPECS.forEach(
  ([testNameFragment, testRef, expected]) => {
    test(`isCoordinatedReleaseTag ${testNameFragment}`, () => {
      expect(action.isCoordinatedReleaseTag(testRef)).toStrictEqual(expected)
    })
  }
)
