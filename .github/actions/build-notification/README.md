# Build Notification

Single entrypoint for oe-core build Slack notifications. Workflows pass context and optionally a `notification_kind`; this action decides whether to send, the payload shape, and which webhook to use.

## Usage

Success notifications run inline in `run-build`:

- **All builds** — `deployed` after artifact upload (tagged → `SLACK_WEBHOOK_ARTIFACTS_DEPLOY_URL_TAGGED`, branch → `SLACK_WEBHOOK_URL`)
- **Tagged builds** — **Build's done!** (`cache-synced`) after S3 cache push via `SLACK_WEBHOOK_URL_TAGGED`

Failure and cancelled notifications run from a separate job at the end of the build workflow.

```yaml
notify-build:
  name: Notify Build Outcome
  runs-on: ubuntu-latest
  needs: [decide-refs, run-build]
  if: always() && (needs.run-build.result == 'failure' || needs.run-build.result == 'cancelled')
  steps:
    - name: Fetch initial sources for action
      uses: actions/checkout@34e114876b0b11c390a56381ad16ebd13914f8d5 # v4.3.1
      with:
        submodules: false
        path: ./oe-core-for-workflow
    - name: Notify build outcome
      uses: ./oe-core-for-workflow/.github/actions/build-notification
      continue-on-error: true
      with:
        workflow: flex-build
        notification_kind: auto
        event_name: ${{ github.event_name }}
        job_result: ${{ needs.run-build.result }}
        webhook_url: ${{ secrets.SLACK_WEBHOOK_URL }}
        webhook_url_tagged: ${{ vars.SLACK_WEBHOOK_URL_TAGGED }}
        workflow_name: Build Flex image on github workflows
        oe_core_ref: ${{ needs.decide-refs.outputs.oe-core }}
        monorepo_ref: ${{ needs.decide-refs.outputs.monorepo }}
        firmware_ref: ${{ needs.decide-refs.outputs.ot3-firmware }}
        variant: ${{ needs.decide-refs.outputs.variant }}
        console_url: ${{ needs.run-build.outputs.console_url }}
        system_url: ${{ needs.run-build.outputs.system_url }}
        fullimage_url: ${{ needs.run-build.outputs.fullimage_url }}
        version_file_url: ${{ needs.run-build.outputs.version_file_url }}
        release_notes_file_url: ${{ needs.run-build.outputs.release_notes_file_url }}
```

## Webhook routing (build-ot3-actions.yml)

| Step | When | Branch builds | Tagged builds |
| --- | --- | --- | --- |
| Notify artifacts deployed | After S3 artifact upload | `SLACK_WEBHOOK_URL` | `SLACK_WEBHOOK_ARTIFACTS_DEPLOY_URL_TAGGED` |
| Notify build complete | After S3 cache push | skip | `SLACK_WEBHOOK_URL_TAGGED` |
| notify-build (failure/cancelled) | End of workflow | `SLACK_WEBHOOK_URL` | `SLACK_WEBHOOK_URL_TAGGED` |

Each step passes the appropriate URL via `webhook_url` (branch) and `webhook_url_tagged` (tagged). The action picks based on whether `monorepo_ref` starts with `refs/tags/`.

## Routing rules (`flex-build`)

Set `notification_kind` to target a specific alert, or `auto` (default) to derive the kind from `job_result`.

| Condition | Notification | Webhook target |
| --- | --- | --- |
| `event_name != workflow_dispatch` | skip | |
| `notification_kind: deployed`, `job_result == success`, branch build | send `deployed` | `webhook_url` |
| `notification_kind: deployed`, tagged build with non-empty `variant` | send `deployed` | `webhook_url_tagged` (fallback: `webhook_url`) |
| `notification_kind: deployed`, tagged build with empty `variant` | skip | |
| `notification_kind: cache-synced`, tagged build with non-empty `variant`, `job_result == success` | send `cache-synced` | `webhook_url_tagged` (fallback: `webhook_url`) |
| `notification_kind: cache-synced`, branch build | skip | |
| `notification_kind: auto`, `job_result == failure` | send `failure` | tagged or default by ref |
| `notification_kind: auto`, `job_result == cancelled` | send `cancelled` | tagged or default by ref |
| other combinations | skip | |

## Slack Workflow setup

Webhook URLs must be Slack Workflow trigger URLs (not legacy Incoming Webhooks).

| GitHub config | Slack workflow |
| --- | --- |
| `secrets.SLACK_WEBHOOK_URL` | Branch builds — deployed, failure, cancelled |
| `vars.SLACK_WEBHOOK_ARTIFACTS_DEPLOY_URL_TAGGED` | Tagged builds — artifacts available (`status: deployed`) |
| `vars.SLACK_WEBHOOK_URL_TAGGED` | Tagged builds — **Build's done!** after cache sync (`status: cache-synced`), plus failure/cancelled |

## Outputs

| Output | Description |
| --- | --- |
| `sent` | `true` when a webhook POST was made |
| `notification_kind` | `deployed`, `cache-synced`, `failure`, `cancelled`, or `none` |
| `webhook_target` | `tagged`, `default`, or `none` |
| `skip_reason` | Why a notification was skipped |
