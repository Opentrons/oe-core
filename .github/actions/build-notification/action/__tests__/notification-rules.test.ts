import {
  buildSlackPayload,
  decideNotification,
  refType,
  resolveWebhookUrl,
  selectWebhookTarget,
  stripRefPrefix,
  tagField,
} from '../notification-rules'

describe('stripRefPrefix', () => {
  test('removes refs/heads prefix', () => {
    expect(stripRefPrefix('refs/heads/edge')).toBe('edge')
  })

  test('removes refs/tags prefix', () => {
    expect(stripRefPrefix('refs/tags/v7.5.0')).toBe('v7.5.0')
  })
})

describe('refType and tagField', () => {
  test('identifies tag builds', () => {
    expect(refType('refs/tags/v7.5.0')).toBe('tag')
    expect(tagField('refs/tags/v7.5.0')).toBe('v7.5.0')
  })

  test('identifies branch builds', () => {
    expect(refType('refs/heads/edge')).toBe('branch')
    expect(tagField('refs/heads/edge')).toBe('None')
  })
})

describe('decideNotification for flex-build', () => {
  const base = {
    workflow: 'flex-build' as const,
    eventName: 'workflow_dispatch',
    monorepoRef: 'refs/heads/edge',
    variant: 'internal-release',
  }

  test('sends deployed for successful branch builds', () => {
    expect(decideNotification({ ...base, jobResult: 'success' })).toMatchObject(
      {
        send: true,
        kind: 'deployed',
        webhookTarget: 'default',
        statusLabel: 'deployed',
      }
    )
  })

  test('sends deployed for successful tagged builds with variant', () => {
    expect(
      decideNotification({
        ...base,
        jobResult: 'success',
        monorepoRef: 'refs/tags/v7.5.0',
        variant: 'release',
      })
    ).toMatchObject({
      send: true,
      kind: 'deployed',
      webhookTarget: 'tagged',
    })
  })

  test('skips deployed for tagged builds without variant', () => {
    expect(
      decideNotification({
        ...base,
        jobResult: 'success',
        monorepoRef: 'refs/tags/v7.5.0',
        variant: '',
      })
    ).toMatchObject({ send: false, kind: null })
  })

  test('sends failure and cancelled notifications', () => {
    expect(decideNotification({ ...base, jobResult: 'failure' })).toMatchObject(
      { send: true, kind: 'failure', webhookTarget: 'default' }
    )
    expect(
      decideNotification({ ...base, jobResult: 'cancelled' })
    ).toMatchObject({ send: true, kind: 'cancelled', webhookTarget: 'default' })
  })

  test('skips non workflow_dispatch events', () => {
    expect(
      decideNotification({ ...base, eventName: 'push', jobResult: 'success' })
    ).toMatchObject({ send: false })
  })

  test('skips skipped job results', () => {
    expect(decideNotification({ ...base, jobResult: 'skipped' })).toMatchObject(
      { send: false }
    )
  })
})

describe('webhook destination routing', () => {
  test('selects tagged vs default webhook targets', () => {
    expect(selectWebhookTarget('refs/tags/v7.5.0')).toBe('tagged')
    expect(selectWebhookTarget('refs/heads/edge')).toBe('default')
  })

  test('resolves tagged webhook with fallback', () => {
    expect(
      resolveWebhookUrl('tagged', 'https://default', 'https://tagged')
    ).toBe('https://tagged')
    expect(resolveWebhookUrl('tagged', 'https://default', '')).toBe(
      'https://default'
    )
    expect(
      resolveWebhookUrl('default', 'https://default', 'https://tagged')
    ).toBe('https://default')
  })
})

describe('buildSlackPayload', () => {
  test('builds the Slack workflow payload', () => {
    const decision = decideNotification({
      workflow: 'flex-build',
      eventName: 'workflow_dispatch',
      jobResult: 'success',
      monorepoRef: 'refs/heads/edge',
      variant: 'internal-release',
    })

    expect(
      buildSlackPayload({
        workflow: 'flex-build',
        eventName: 'workflow_dispatch',
        jobResult: 'success',
        monorepoRef: 'refs/heads/edge',
        variant: 'internal-release',
        oeCoreRef: 'refs/heads/main',
        firmwareRef: 'refs/heads/main',
        consoleUrl: 'https://example.com/console',
        systemUrl: 'https://example.com/system.zip',
        fullImageUrl: 'https://example.com/full.tar',
        versionFileUrl: 'https://example.com/VERSION.json',
        releaseNotesUrl: 'https://example.com/release-notes.md',
        workflowName: 'Build Flex image on github workflows',
        workflowRunUrl: 'https://github.com/Opentrons/oe-core/actions/runs/1',
        failedJobs: '',
        decision,
      })
    ).toEqual({
      tag: 'None',
      headline: 'Build artifacts deployed',
      status: 'deployed',
      's3-url': 'https://example.com/console/',
      type: 'branch',
      reflike: 'main',
      'monorepo-reflike': 'edge',
      'firmware-reflike': 'main',
      'full-image': 'https://example.com/full.tar',
      'system-update': 'https://example.com/system.zip',
      'version-file': 'https://example.com/VERSION.json',
      'release-notes': 'https://example.com/release-notes.md',
      'workflow-name': 'Build Flex image on github workflows',
      'workflow-run-url': 'https://github.com/Opentrons/oe-core/actions/runs/1',
      'failed-jobs': '',
    })
  })
})
