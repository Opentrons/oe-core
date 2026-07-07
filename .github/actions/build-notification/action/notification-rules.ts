export type JobResult = 'success' | 'failure' | 'cancelled' | 'skipped'
export type NotificationKind = 'deployed' | 'failure' | 'cancelled'
export type WorkflowKind = 'flex-build'
export type WebhookTarget = 'tagged' | 'default'

export interface NotificationContext {
  workflow: WorkflowKind
  eventName: string
  jobResult: JobResult
  monorepoRef: string
  variant: string
}

export type NotificationDecision =
  | {
      send: true
      kind: NotificationKind
      webhookTarget: WebhookTarget
      skipReason: null
      headline: string
      statusLabel: NotificationKind
    }
  | {
      send: false
      kind: null
      webhookTarget: null
      skipReason: string
      headline: null
      statusLabel: null
    }

export interface ArtifactUrls {
  consoleUrl: string
  systemUrl: string
  fullImageUrl: string
  versionFileUrl: string
  releaseNotesUrl: string
}

export interface BuildRefs {
  oeCoreRef: string
  monorepoRef: string
  firmwareRef: string
}

export interface SlackPayloadInput
  extends NotificationContext, ArtifactUrls, BuildRefs {
  decision: NotificationDecision
  workflowName: string
  workflowRunUrl: string
  failedJobs: string
}

export type SlackPayload = Record<string, string>

const HEADLINES: Record<NotificationKind, string> = {
  deployed: 'Build artifacts deployed',
  failure: 'Build failed',
  cancelled: 'Build cancelled',
}

export function stripRefPrefix(ref: string): string {
  return ref.replace(/^refs\/(?:tags|heads)\//, '')
}

export function refType(monorepoRef: string): 'tag' | 'branch' {
  return monorepoRef.startsWith('refs/tags/') ? 'tag' : 'branch'
}

export function tagField(monorepoRef: string): string {
  return monorepoRef.startsWith('refs/tags/')
    ? stripRefPrefix(monorepoRef)
    : 'None'
}

export function isTaggedBuild(monorepoRef: string): boolean {
  return monorepoRef.startsWith('refs/tags/')
}

export function selectWebhookTarget(monorepoRef: string): WebhookTarget {
  return isTaggedBuild(monorepoRef) ? 'tagged' : 'default'
}

export function resolveWebhookUrl(
  webhookTarget: WebhookTarget,
  defaultWebhookUrl: string,
  taggedWebhookUrl: string
): string {
  if (webhookTarget === 'tagged' && taggedWebhookUrl !== '') {
    return taggedWebhookUrl
  }
  return defaultWebhookUrl
}

export function decideNotification(
  context: NotificationContext
): NotificationDecision {
  return decideFlexBuildNotification(context)
}

function decideFlexBuildNotification(
  context: NotificationContext
): NotificationDecision {
  if (context.eventName !== 'workflow_dispatch') {
    return skipDecision(
      `flex-build notifications only run on workflow_dispatch (got ${context.eventName})`
    )
  }

  const webhookTarget = selectWebhookTarget(context.monorepoRef)

  switch (context.jobResult) {
    case 'success': {
      if (isTaggedBuild(context.monorepoRef) && context.variant === '') {
        return skipDecision(
          'tagged build without variant does not send deployed notifications'
        )
      }
      return sendDecision('deployed', webhookTarget)
    }
    case 'failure':
      return sendDecision('failure', webhookTarget)
    case 'cancelled':
      return sendDecision('cancelled', webhookTarget)
    default:
      return skipDecision(
        `job result ${context.jobResult} does not trigger notifications`
      )
  }
}

function sendDecision(
  kind: NotificationKind,
  webhookTarget: WebhookTarget
): NotificationDecision {
  return {
    send: true,
    kind,
    webhookTarget,
    skipReason: null,
    headline: HEADLINES[kind],
    statusLabel: kind,
  }
}

function skipDecision(reason: string): NotificationDecision {
  return {
    send: false,
    kind: null,
    webhookTarget: null,
    skipReason: reason,
    headline: null,
    statusLabel: null,
  }
}

export function buildSlackPayload(input: SlackPayloadInput): SlackPayload {
  const { decision } = input
  if (!decision.send) {
    throw new Error('buildSlackPayload requires a send decision')
  }

  const consoleUrl = input.consoleUrl === '' ? '' : `${input.consoleUrl}/`

  return {
    tag: tagField(input.monorepoRef),
    headline: decision.headline,
    status: decision.statusLabel,
    's3-url': consoleUrl,
    type: refType(input.monorepoRef),
    reflike: stripRefPrefix(input.oeCoreRef),
    'monorepo-reflike': stripRefPrefix(input.monorepoRef),
    'firmware-reflike': stripRefPrefix(input.firmwareRef),
    'full-image': input.fullImageUrl,
    'system-update': input.systemUrl,
    'version-file': input.versionFileUrl,
    'release-notes': input.releaseNotesUrl,
    'workflow-name': input.workflowName,
    'workflow-run-url': input.workflowRunUrl,
    'failed-jobs': input.failedJobs,
  }
}
