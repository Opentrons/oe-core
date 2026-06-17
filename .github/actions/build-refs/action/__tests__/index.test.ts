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
  [string, [Ref, boolean, Branch], Ref[], action.Repo?]
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
  [
    'with external firmware tags on ot3-firmware maps to ex tag',
    ['refs/tags/v10.0.0-beta.1', false, 'refs/heads/someDefaultBranch'],
    ['refs/tags/ex10.0.0-beta.1'],
    'ot3-firmware',
  ],
  [
    'with internal ot3 tags on ot3-firmware uses same tag',
    ['refs/tags/ot3@8.5.0-alpha.0', false, 'refs/heads/someDefaultBranch'],
    ['refs/tags/ot3@8.5.0-alpha.0'],
    'ot3-firmware',
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
    repo,
  ]) => {
    test(`refsToAttempt ${testNameFragment}`, () => {
      expect(
        action.refsToAttempt(
          testRequesterRef,
          testRequesterIsDefaultBranch,
          testRequestedDefaultBranch,
          repo
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

const COORDINATED_RELEASE_TAG_SPECS: Array<[string, Ref, boolean]> = [
  ['ot3 internal alpha', 'refs/tags/ot3@8.5.0-alpha.0', true],
  ['ot3 internal beta', 'refs/tags/ot3@8.5.0-beta.1', true],
  ['ot3 internal stable', 'refs/tags/ot3@8.5.0', true],
  ['external beta', 'refs/tags/v10.0.0-beta.1', true],
  ['external alpha', 'refs/tags/v10.0.0-alpha.0', true],
  ['external stable', 'refs/tags/v10.0.0', true],
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

const FIRMWARE_EX_TAG_MAPPING_SPECS: Array<[string, Ref, Ref | null]> = [
  [
    'external semver alpha',
    'refs/tags/v9.1.0-alpha.7',
    'refs/tags/ex9.1.0-alpha.7',
  ],
  ['external semver stable', 'refs/tags/v9.1.0', 'refs/tags/ex9.1.0'],
  ['internal ot3 alpha', 'refs/tags/ot3@8.5.0-alpha.1', null],
  ['internal ot3 stable', 'refs/tags/ot3@8.5.0', null],
  ['internal semver on oe-core', 'refs/tags/internal@8.5.0-alpha.1', null],
  ['integer firmware version v70', 'refs/tags/v70', null],
  ['existing ex tag', 'refs/tags/ex9.1.0-alpha.7', null],
  ['branch ref', 'refs/heads/main', null],
]

FIRMWARE_EX_TAG_MAPPING_SPECS.forEach(
  ([testNameFragment, stackTag, expected]) => {
    test(`stackCoordinatedTagToFirmwareTag ${testNameFragment}`, () => {
      expect(action.stackCoordinatedTagToFirmwareTag(stackTag)).toStrictEqual(
        expected
      )
    })
  }
)

test('normalizeFirmwareInputRef maps external stack v tags to ex tags', () => {
  expect(
    action.normalizeFirmwareInputRef('refs/tags/v9.1.0-alpha.7')
  ).toStrictEqual('refs/tags/ex9.1.0-alpha.7')
})

test('normalizeFirmwareInputRef leaves internal ot3 tags unchanged', () => {
  expect(
    action.normalizeFirmwareInputRef('refs/tags/ot3@8.5.0-alpha.1')
  ).toStrictEqual('refs/tags/ot3@8.5.0-alpha.1')
})

test('normalizeFirmwareInputRef leaves integer vN tags unchanged', () => {
  expect(action.normalizeFirmwareInputRef('refs/tags/v70')).toStrictEqual(
    'refs/tags/v70'
  )
})

test('normalizeFirmwareInputRef leaves branch refs unchanged', () => {
  expect(action.normalizeFirmwareInputRef('refs/heads/main')).toStrictEqual(
    'refs/heads/main'
  )
})

test('expectedFirmwareTagForAuthoritative maps external semver', () => {
  expect(
    action.expectedFirmwareTagForAuthoritative('refs/tags/v9.1.0-alpha.7')
  ).toStrictEqual('refs/tags/ex9.1.0-alpha.7')
})

test('expectedFirmwareTagForAuthoritative keeps internal ot3 tag', () => {
  expect(
    action.expectedFirmwareTagForAuthoritative('refs/tags/ot3@8.5.0-alpha.1')
  ).toStrictEqual('refs/tags/ot3@8.5.0-alpha.1')
})

const FIRMWARE_VERSION_TAG_SPECS: Array<[string, Ref, boolean]> = [
  ['integer v70', 'refs/tags/v70', true],
  ['integer v9', 'refs/tags/v9', true],
  ['semver coordination', 'refs/tags/v9.1.0-alpha.7', false],
  ['ex coordination tag', 'refs/tags/ex9.1.0-alpha.7', false],
  ['branch', 'refs/heads/main', false],
]

FIRMWARE_VERSION_TAG_SPECS.forEach(([testNameFragment, testRef, expected]) => {
  test(`isFirmwareVersionTagRef ${testNameFragment}`, () => {
    expect(action.isFirmwareVersionTagRef(testRef)).toStrictEqual(expected)
  })
})
