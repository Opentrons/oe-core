# Build Notification

Single entrypoint for oe-core build Slack notifications. Workflows pass all context once at the end; this action decides whether to send, the payload shape, and which webhook to use.

## Usage

```yaml
notify-build:
  name: Notify Build Outcome
  runs-on: ubuntu-latest
  needs: [decide-refs, run-build]
  if: always()
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

The workflow job should always run (`if: always()`). All filtering happens inside the action.

## Routing rules (`flex-build`)

| Condition | Notification | Webhook |
| --- | --- | --- |
| `event_name != workflow_dispatch` | skip | |
| `job_result == success` and branch build | send `deployed` | `webhook_url` |
| `job_result == success` and tagged build with non-empty `variant` | send `deployed` | `webhook_url_tagged` (fallback: `webhook_url`) |
| `job_result == success` and tagged build with empty `variant` | skip | |
| `job_result == failure` | send `failure` | tagged or default by ref |
| `job_result == cancelled` | send `cancelled` | tagged or default by ref |
| other job results | skip | |

Payload shape varies by notification kind (`headline`, `status`, artifact URLs, refs, `tag` field).

## Outputs

| Output | Description |
| --- | --- |
| `sent` | `true` when a webhook POST was made |
| `notification_kind` | `deployed`, `failure`, `cancelled`, or `none` |
| `webhook_target` | `tagged`, `default`, or `none` |
| `skip_reason` | Why a notification was skipped |
