import * as action from '../index'
import type { Repo, InputRefs, Ref, Branch } from '../index'

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
    'identifies if monorepo preferred but on non-main branch',
    new Map([
      ['oe-core', 'refs/heads/main'],
      ['monorepo', 'refs/heads/some-test-branch'],
    ]),
    ['refs/heads/some-test-branch', false],
  ],
  [
    'identifies if oe-core preferred but on non-main branch',
    new Map([
      ['oe-core', 'refs/heads/some-test-branch'],
      ['monorepo', null],
    ]),
    ['refs/heads/some-test-branch', false],
  ],
  [
    'falls back to main branches if nothing is specified',
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
    'with non-main branches should prioritize the request and then use main',
    ['refs/heads/someBranch', false, 'refs/heads/someMain'],
    ['refs/heads/someBranch', 'refs/heads/someMain'],
  ],
  [
    'with main branches should just use main',
    ['refs/heads/someOtherMain', true, 'refs/heads/someMain'],
    ['refs/heads/someMain'],
  ],
  [
    'with tags should prioritize match tag, then latest',
    ['refs/tags/someTag', false, 'refs/heads/someMain'],
    ['refs/tags/someTag', ':latest:', 'refs/heads/someMain'],
  ],
]

REFS_TO_ATTEMPT_TEST_SPECS.forEach(
  ([
    testNameFragment,
    [testRequesterRef, testRequesterIsMain, testRequestedMain],
    testResults,
  ]) => {
    test(`refsToAttempt ${testNameFragment}`, () => {
      expect(
        action.refsToAttempt(
          testRequesterRef,
          testRequesterIsMain,
          testRequestedMain
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
    ['shortBranchName', true, 'refs/heads/someMain'],
  ],
]

REFS_TO_ATTEMPT_FAILURE_TEST_SPECS.forEach(
  ([
    testNameFragment,
    [testRequesterRef, testRequesterIsMain, testRequestedMain],
  ]) => {
    test(`refsToAttempt ${testNameFragment}`, () => {
      expect(() => {
        action.refsToAttempt(
          testRequesterRef,
          testRequesterIsMain,
          testRequestedMain
        )
      }).toThrow()
    })
  }
)
