import * as core from '@actions/core'
import {
  buildSlackPayload,
  decideNotification,
  resolveWebhookUrl,
  type JobResult,
  type WorkflowKind,
} from './notification-rules'

export * from './notification-rules'

function readInput(name: string, required = false): string {
  const value = core.getInput(name, { required }).trim()
  return value
}

function isJobResult(value: string): value is JobResult {
  return (
    value === 'success' ||
    value === 'failure' ||
    value === 'cancelled' ||
    value === 'skipped'
  )
}

function parseJobResult(raw: string): JobResult {
  if (!isJobResult(raw)) {
    throw new Error(
      `Invalid job_result "${raw}". Expected one of: success, failure, cancelled, skipped`
    )
  }
  return raw
}

function parseWorkflow(raw: string): WorkflowKind {
  if (raw !== 'flex-build') {
    throw new Error(`Invalid workflow "${raw}". Expected: flex-build`)
  }
  return raw
}

function defaultWorkflowRunUrl(): string {
  const server = process.env.GITHUB_SERVER_URL ?? 'https://github.com'
  const repository = process.env.GITHUB_REPOSITORY ?? ''
  const runId = process.env.GITHUB_RUN_ID ?? ''
  return `${server}/${repository}/actions/runs/${runId}`
}

async function postToSlackWebhook(
  webhookUrl: string,
  payload: Record<string, string>
): Promise<void> {
  const response = await fetch(webhookUrl, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })

  if (!response.ok) {
    const body = await response.text()
    const errorBody = body !== '' ? body : response.statusText
    throw new Error(
      `Slack webhook request failed (${response.status}): ${errorBody}`
    )
  }
}

async function run(): Promise<void> {
  const workflow = parseWorkflow(readInput('workflow', true))
  const eventName = readInput('event_name', true)
  const jobResult = parseJobResult(readInput('job_result', true))
  const defaultWebhookUrl = readInput('webhook_url')
  const taggedWebhookUrl = readInput('webhook_url_tagged')
  const dryRun = readInput('dry_run') === 'true'

  const context = {
    workflow,
    eventName,
    jobResult,
    monorepoRef: readInput('monorepo_ref'),
    variant: readInput('variant'),
  }

  const decision = decideNotification(context)

  core.setOutput('sent', decision.send ? 'true' : 'false')
  core.setOutput('notification_kind', decision.send ? decision.kind : 'none')
  core.setOutput(
    'webhook_target',
    decision.send ? decision.webhookTarget : 'none'
  )
  core.setOutput('skip_reason', decision.send ? '' : decision.skipReason)

  if (!decision.send) {
    core.info(decision.skipReason)
    return
  }

  const webhookUrl = resolveWebhookUrl(
    decision.webhookTarget,
    defaultWebhookUrl,
    taggedWebhookUrl
  )

  if (webhookUrl === '') {
    throw new Error(
      'webhook_url is required when notification routing selects send=true'
    )
  }

  const workflowRunUrlInput = readInput('workflow_run_url')
  const payload = buildSlackPayload({
    ...context,
    decision,
    oeCoreRef: readInput('oe_core_ref'),
    firmwareRef: readInput('firmware_ref'),
    consoleUrl: readInput('console_url'),
    systemUrl: readInput('system_url'),
    fullImageUrl: readInput('fullimage_url'),
    versionFileUrl: readInput('version_file_url'),
    releaseNotesUrl: readInput('release_notes_file_url'),
    workflowName: readInput('workflow_name'),
    workflowRunUrl:
      workflowRunUrlInput !== ''
        ? workflowRunUrlInput
        : defaultWorkflowRunUrl(),
    failedJobs: readInput('failed_jobs'),
  })

  core.info(
    `Sending ${decision.kind} notification for ${workflow} via ${decision.webhookTarget} webhook`
  )
  core.debug(JSON.stringify(payload))

  if (dryRun) {
    core.info('dry_run=true; skipping Slack webhook POST')
    return
  }

  await postToSlackWebhook(webhookUrl, payload)
}

run().catch((error: unknown) => {
  if (error instanceof Error) {
    core.setFailed(error.message)
  } else {
    core.setFailed(String(error))
  }
})
